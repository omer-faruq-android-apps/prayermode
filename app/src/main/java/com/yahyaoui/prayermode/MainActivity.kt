package com.yahyaoui.prayermode

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.yahyaoui.prayermode.TermsAndConditions.TermsAndConditionsListener
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.yahyaoui.prayermode.TermsAndConditions.Companion.TERMS_URL
import android.Manifest
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

private const val IS_APP_RESTARTED_KEY = "isAppRestarted"

class MainActivity : AppCompatActivity(), TermsAndConditionsListener, WelcomeDialog.WelcomeDialogListener {

    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleHelper.getPersistedLocale()
        if (BuildConfig.DEBUG) Log.d("MainActivity", "Attaching base context for locale: $locale")
        super.attachBaseContext(LocaleHelper.setLocale(newBase, locale))
    }

    private lateinit var activateSwitch: SwitchCompat
    private lateinit var switchButtonContainer: View
    private lateinit var tvSwitchState: TextView
    private lateinit var calculationMethodsContainer: View
    private lateinit var tvCalculationMethod: TextView
    private lateinit var silentModeTypeContainer: View
    private lateinit var tvSilentModeType: TextView
    private lateinit var beforeDhuhrContainer: View
    private lateinit var tvBeforeDhuhr: TextView
    private lateinit var afterDhuhrContainer: View
    private lateinit var tvAfterDhuhr: TextView
    private lateinit var tarawihContainer: View
    private lateinit var tvTarawih: TextView
    private lateinit var tahajjudContainer: View
    private lateinit var tvTahajjud: TextView
    private lateinit var eidTimeContainer: View
    private lateinit var tvEidTime: TextView
    private lateinit var eidDurationContainer: View
    private lateinit var tvEidDuration: TextView
    private lateinit var privacyContainer: MaterialCardView
    private lateinit var termsContainer: MaterialCardView
    private lateinit var permissionsContainer: MaterialCardView
    private lateinit var timesContainer: MaterialCardView
    private lateinit var helpContainer: MaterialCardView
    private lateinit var donationContainer: MaterialCardView

    private lateinit var audioSwitch: SwitchCompat
    private lateinit var audioSwitchContainer: View

    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var notificationSwitchContainer: View

    private lateinit var fajrBeforeContainer: View
    private lateinit var tvFajrBefore: TextView
    private lateinit var fajrAfterContainer: View
    private lateinit var tvFajrAfter: TextView
    private lateinit var sunriseBeforeContainer: View
    private lateinit var tvSunriseBefore: TextView
    private lateinit var sunriseAfterContainer: View
    private lateinit var tvSunriseAfter: TextView
    private lateinit var dhuhrBeforeContainer: View
    private lateinit var tvDhuhrBefore: TextView
    private lateinit var dhuhrAfterContainer: View
    private lateinit var tvDhuhrAfter: TextView
    private lateinit var asrBeforeContainer: View
    private lateinit var tvAsrBefore: TextView
    private lateinit var asrAfterContainer: View
    private lateinit var tvAsrAfter: TextView
    private lateinit var maghribBeforeContainer: View
    private lateinit var tvMaghribBefore: TextView
    private lateinit var maghribAfterContainer: View
    private lateinit var tvMaghribAfter: TextView
    private lateinit var ishaBeforeContainer: View
    private lateinit var tvIshaBefore: TextView
    private lateinit var ishaAfterContainer: View
    private lateinit var tvIshaAfter: TextView

    private val tools: Tools by lazy { Tools(this) }
    private val sharedHelper: SharedHelper by lazy { SharedHelper(this) }
    private val permissionsHelper: PermissionsHelper by lazy { PermissionsHelper(this) }
    private val tag = "MainActivity"
    private var isAppRestarted = true
    private var isRestoringSwitchState = false

    private val locationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) handleLocationGranted() else showLocationDenied()
    }
    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) handleNotificationGranted() else showNotificationDenied()
    }
    private val dndStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED) {
                checkDndAndUpdateSwitch()
            }
        }
    }
    private val calculationMethodLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedMethodIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.SELECTED_METHOD_RES_ID, selectedMethodIndex)
            tvCalculationMethod.text = sharedHelper.getStringFromArray(R.array.calculation_methods, SharedHelper.SELECTED_METHOD_RES_ID, 3)
        }
    }
    private val silentModeTypeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.SILENT_MODE_TYPE, selectedIndex)
            tvSilentModeType.text = sharedHelper.getStringFromArray(R.array.silent_mode_types, SharedHelper.SILENT_MODE_TYPE, 0)
        }
    }
    private val durationBeforeDhuhrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedDurationBeforeDhuhrIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_DHUHR, selectedDurationBeforeDhuhrIndex)
            tvBeforeDhuhr.text = sharedHelper.getStringFromArray(R.array.before_dhuhr_duration, SharedHelper.DURATION_BEFORE_DHUHR, 0)
        }
    }
    private val durationAfterDhuhrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedDurationAfterDhuhrIndex = result.data?.getIntExtra("SELECTED_INDEX", 3) ?: 3
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_DHUHR, selectedDurationAfterDhuhrIndex)
            tvAfterDhuhr.text = sharedHelper.getStringFromArray(R.array.after_dhuhr_duration, SharedHelper.DURATION_AFTER_DHUHR, 3)
        }
    }
    private val durationTarawihLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedDurationTarawihIndex = result.data?.getIntExtra("SELECTED_INDEX", 4) ?: 4
            sharedHelper.saveIntValue(SharedHelper.DURATION_TARAWIH, selectedDurationTarawihIndex)
            tvTarawih.text = sharedHelper.getStringFromArray(R.array.tarawih_duration, SharedHelper.DURATION_TARAWIH, 4)
        }
    }
    private val durationTahajjudLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedDurationTahajjudIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.DURATION_TAHAJJUD, selectedDurationTahajjudIndex)
            tvTahajjud.text = sharedHelper.getStringFromArray(R.array.tahajjud_duration, SharedHelper.DURATION_TAHAJJUD, 0)
        }
    }
    private val eidTimeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedEidTimeIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.SELECTED_TIME_EID, selectedEidTimeIndex)
            tvEidTime.text = sharedHelper.getStringFromArray(R.array.eid_time, SharedHelper.SELECTED_TIME_EID, 0)
        }
    }
    private val eidDurationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedEidDurationIndex = result.data?.getIntExtra("SELECTED_INDEX", 1) ?: 1
            sharedHelper.saveIntValue(SharedHelper.DURATION_EID, selectedEidDurationIndex)
            tvEidDuration.text = sharedHelper.getStringFromArray(R.array.eid_duration, SharedHelper.DURATION_EID, 1)
        }
    }

    private val fajrBeforeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 2) ?: 2
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_FAJR, selectedIndex)
            tvFajrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_FAJR, 2)
        }
    }
    private val fajrAfterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 4) ?: 4
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_FAJR, selectedIndex)
            tvFajrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_FAJR, 4)
        }
    }
    private val sunriseBeforeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_SUNRISE, selectedIndex)
            tvSunriseBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_SUNRISE, 0)
        }
    }
    private val sunriseAfterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 0) ?: 0
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_SUNRISE, selectedIndex)
            tvSunriseAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_SUNRISE, 0)
        }
    }
    private val dhuhrBeforeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 2) ?: 2
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_DHUHR_DAILY, selectedIndex)
            tvDhuhrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_DHUHR_DAILY, 2)
        }
    }
    private val dhuhrAfterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 4) ?: 4
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_DHUHR_DAILY, selectedIndex)
            tvDhuhrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_DHUHR_DAILY, 4)
        }
    }
    private val asrBeforeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 2) ?: 2
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_ASR, selectedIndex)
            tvAsrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ASR, 2)
        }
    }
    private val asrAfterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 4) ?: 4
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_ASR, selectedIndex)
            tvAsrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ASR, 4)
        }
    }
    private val maghribBeforeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 2) ?: 2
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_MAGHRIB, selectedIndex)
            tvMaghribBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_MAGHRIB, 2)
        }
    }
    private val maghribAfterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 4) ?: 4
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_MAGHRIB, selectedIndex)
            tvMaghribAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_MAGHRIB, 4)
        }
    }
    private val ishaBeforeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 2) ?: 2
            sharedHelper.saveIntValue(SharedHelper.DURATION_BEFORE_ISHA, selectedIndex)
            tvIshaBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ISHA, 2)
        }
    }
    private val ishaAfterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIndex = result.data?.getIntExtra("SELECTED_INDEX", 4) ?: 4
            sharedHelper.saveIntValue(SharedHelper.DURATION_AFTER_ISHA, selectedIndex)
            tvIshaAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ISHA, 4)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        activateSwitch = findViewById(R.id.activateSwitch)
        tvSwitchState = findViewById(R.id.tvSwitchState)
        tvCalculationMethod = findViewById(R.id.tvCalculationMethod)
        tvBeforeDhuhr = findViewById(R.id.tvBeforeDhuhr)
        tvAfterDhuhr = findViewById(R.id.tvAfterDhuhr)
        tvTarawih = findViewById(R.id.tvTarawih)
        tvTahajjud = findViewById(R.id.tvTahajjud)
        tvEidTime = findViewById(R.id.tvEidTime)
        tvEidDuration = findViewById(R.id.tvEidDuration)
        switchButtonContainer = findViewById(R.id.switchButtonContainer)
        calculationMethodsContainer = findViewById(R.id.calculationMethodsContainer)
        silentModeTypeContainer = findViewById(R.id.silentModeTypeContainer)
        tvSilentModeType = findViewById(R.id.tvSilentModeType)
        beforeDhuhrContainer = findViewById(R.id.beforeDhuhrContainer)
        afterDhuhrContainer = findViewById(R.id.afterDhuhrContainer)
        tarawihContainer = findViewById(R.id.tarawihContainer)
        tahajjudContainer = findViewById(R.id.tahajjudContainer)
        eidTimeContainer = findViewById(R.id.eidTimeContainer)
        eidDurationContainer = findViewById(R.id.eidDurationContainer)
        privacyContainer = findViewById(R.id.privacyContainer)
        termsContainer = findViewById(R.id.termsContainer)
        permissionsContainer = findViewById(R.id.permissionsContainer)
        timesContainer = findViewById(R.id.timesContainer)
        helpContainer = findViewById(R.id.helpContainer)
        donationContainer = findViewById(R.id.donationContainer)
        audioSwitch = findViewById(R.id.audioSwitch)
        audioSwitchContainer = findViewById(R.id.audioSwitchContainer)

        notificationSwitch = findViewById(R.id.notificationSwitch)
        notificationSwitchContainer = findViewById(R.id.notificationSwitchContainer)

        fajrBeforeContainer = findViewById(R.id.fajrBeforeContainer)
        tvFajrBefore = findViewById(R.id.tvFajrBefore)
        fajrAfterContainer = findViewById(R.id.fajrAfterContainer)
        tvFajrAfter = findViewById(R.id.tvFajrAfter)
        sunriseBeforeContainer = findViewById(R.id.sunriseBeforeContainer)
        tvSunriseBefore = findViewById(R.id.tvSunriseBefore)
        sunriseAfterContainer = findViewById(R.id.sunriseAfterContainer)
        tvSunriseAfter = findViewById(R.id.tvSunriseAfter)
        dhuhrBeforeContainer = findViewById(R.id.dhuhrBeforeContainer)
        tvDhuhrBefore = findViewById(R.id.tvDhuhrBefore)
        dhuhrAfterContainer = findViewById(R.id.dhuhrAfterContainer)
        tvDhuhrAfter = findViewById(R.id.tvDhuhrAfter)
        asrBeforeContainer = findViewById(R.id.asrBeforeContainer)
        tvAsrBefore = findViewById(R.id.tvAsrBefore)
        asrAfterContainer = findViewById(R.id.asrAfterContainer)
        tvAsrAfter = findViewById(R.id.tvAsrAfter)
        maghribBeforeContainer = findViewById(R.id.maghribBeforeContainer)
        tvMaghribBefore = findViewById(R.id.tvMaghribBefore)
        maghribAfterContainer = findViewById(R.id.maghribAfterContainer)
        tvMaghribAfter = findViewById(R.id.tvMaghribAfter)
        ishaBeforeContainer = findViewById(R.id.ishaBeforeContainer)
        tvIshaBefore = findViewById(R.id.tvIshaBefore)
        ishaAfterContainer = findViewById(R.id.ishaAfterContainer)
        tvIshaAfter = findViewById(R.id.tvIshaAfter)

        titlePadding()
        layoutDirection()

        if (!sharedHelper.getTermsAccepted()) showWelcomeDialog() else requestLocationPermission()
        if (savedInstanceState != null) isAppRestarted = savedInstanceState.getBoolean(IS_APP_RESTARTED_KEY, true)
        if (intent.getBooleanExtra("TOGGLE_SWITCH_TWICE", false)) {
            sharedHelper.saveSwitchState(false)
            activateSwitch.isChecked = false
            Handler(Looper.getMainLooper()).postDelayed({
                sharedHelper.saveSwitchState(true)
                activateSwitch.isChecked = true
            }, 500)
        }

        tvCalculationMethod.text = sharedHelper.getStringFromArray(R.array.calculation_methods, SharedHelper.SELECTED_METHOD_RES_ID, 0)
        tvSilentModeType.text = sharedHelper.getStringFromArray(R.array.silent_mode_types, SharedHelper.SILENT_MODE_TYPE, 0)
        tvBeforeDhuhr.text = sharedHelper.getStringFromArray(R.array.before_dhuhr_duration, SharedHelper.DURATION_BEFORE_DHUHR, 0)
        tvAfterDhuhr.text = sharedHelper.getStringFromArray(R.array.after_dhuhr_duration, SharedHelper.DURATION_AFTER_DHUHR, 3)
        tvTarawih.text = sharedHelper.getStringFromArray(R.array.tarawih_duration, SharedHelper.DURATION_TARAWIH, 4)
        tvTahajjud.text = sharedHelper.getStringFromArray(R.array.tahajjud_duration, SharedHelper.DURATION_TAHAJJUD, 0)
        tvEidTime.text = sharedHelper.getStringFromArray(R.array.eid_time, SharedHelper.SELECTED_TIME_EID, 0)
        tvEidDuration.text = sharedHelper.getStringFromArray(R.array.eid_duration, SharedHelper.DURATION_EID, 1)
        tvFajrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_FAJR, 2)
        tvFajrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_FAJR, 4)
        tvSunriseBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_SUNRISE, 0)
        tvSunriseAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_SUNRISE, 0)
        tvDhuhrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_DHUHR_DAILY, 2)
        tvDhuhrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_DHUHR_DAILY, 4)
        tvAsrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ASR, 2)
        tvAsrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ASR, 4)
        tvMaghribBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_MAGHRIB, 2)
        tvMaghribAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_MAGHRIB, 4)
        tvIshaBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ISHA, 2)
        tvIshaAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ISHA, 4)

        setupListeners()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1001)
    }

    private fun setupListeners() {
        switchButtonContainer.setOnClickListener { activateSwitch.toggle() }
        activateSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isRestoringSwitchState) return@setOnCheckedChangeListener
            sharedHelper.saveSwitchState(isChecked)
            if (isChecked) switchOn() else switchOff()
        }

        audioSwitchContainer.setOnClickListener { audioSwitch.toggle() }
        audioSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedHelper.saveAudioSwitchState(isChecked)
            if (BuildConfig.DEBUG) Log.d(tag, "Audio takbir changed to: $isChecked")
        }

        notificationSwitchContainer.setOnClickListener { notificationSwitch.toggle() }
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedHelper.saveNotificationSwitchState(isChecked)
            if (BuildConfig.DEBUG) Log.d(tag, "Notification setting changed to: $isChecked")
        }

        handleContainerClick(calculationMethodsContainer, R.string.select_calculation_method, R.array.calculation_methods, SharedHelper.SELECTED_METHOD_RES_ID, 0, calculationMethodLauncher)
        handleContainerClick(silentModeTypeContainer, R.string.select_silent_mode_type, R.array.silent_mode_types, SharedHelper.SILENT_MODE_TYPE, 0, silentModeTypeLauncher)
        handleContainerClick(beforeDhuhrContainer, R.string.select_before_dhuhr_duration, R.array.before_dhuhr_duration, SharedHelper.DURATION_BEFORE_DHUHR, 0, durationBeforeDhuhrLauncher)
        handleContainerClick(afterDhuhrContainer, R.string.select_after_dhuhr_duration, R.array.after_dhuhr_duration, SharedHelper.DURATION_AFTER_DHUHR, 3, durationAfterDhuhrLauncher)
        handleContainerClick(tarawihContainer, R.string.select_tarawih_duration, R.array.tarawih_duration, SharedHelper.DURATION_TARAWIH, 4, durationTarawihLauncher)
        handleContainerClick(tahajjudContainer, R.string.select_tahajjud_duration, R.array.tahajjud_duration, SharedHelper.DURATION_TAHAJJUD, 0, durationTahajjudLauncher)
        handleContainerClick(eidTimeContainer, R.string.select_eid_time, R.array.eid_time, SharedHelper.SELECTED_TIME_EID, 0, eidTimeLauncher)
        handleContainerClick(eidDurationContainer, R.string.select_eid_duration, R.array.eid_duration, SharedHelper.DURATION_EID, 1, eidDurationLauncher)

        handleContainerClick(fajrBeforeContainer, R.string.select_before_prayer_duration, R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_FAJR, 2, fajrBeforeLauncher)
        handleContainerClick(fajrAfterContainer, R.string.select_after_prayer_duration, R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_FAJR, 4, fajrAfterLauncher)
        handleContainerClick(sunriseBeforeContainer, R.string.select_before_prayer_duration, R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_SUNRISE, 0, sunriseBeforeLauncher)
        handleContainerClick(sunriseAfterContainer, R.string.select_after_prayer_duration, R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_SUNRISE, 0, sunriseAfterLauncher)
        handleContainerClick(dhuhrBeforeContainer, R.string.select_before_prayer_duration, R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_DHUHR_DAILY, 2, dhuhrBeforeLauncher)
        handleContainerClick(dhuhrAfterContainer, R.string.select_after_prayer_duration, R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_DHUHR_DAILY, 4, dhuhrAfterLauncher)
        handleContainerClick(asrBeforeContainer, R.string.select_before_prayer_duration, R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ASR, 2, asrBeforeLauncher)
        handleContainerClick(asrAfterContainer, R.string.select_after_prayer_duration, R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ASR, 4, asrAfterLauncher)
        handleContainerClick(maghribBeforeContainer, R.string.select_before_prayer_duration, R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_MAGHRIB, 2, maghribBeforeLauncher)
        handleContainerClick(maghribAfterContainer, R.string.select_after_prayer_duration, R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_MAGHRIB, 4, maghribAfterLauncher)
        handleContainerClick(ishaBeforeContainer, R.string.select_before_prayer_duration, R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ISHA, 2, ishaBeforeLauncher)
        handleContainerClick(ishaAfterContainer, R.string.select_after_prayer_duration, R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ISHA, 4, ishaAfterLauncher)

        setupContainer(R.id.privacyContainer, R.string.privacy_notice, R.string.privacy_content, "PRIVACY")
        setupContainer(R.id.permissionsContainer, R.string.permission, R.string.permissions_content, "PERMISSIONS")
        setupContainer(R.id.helpContainer, R.string.help, R.string.bulb_content, "HELP")
        setupContainer(R.id.donationContainer, R.string.donation, R.string.donation_content, "DONATION")

        timesContainer.setOnClickListener {
            val intent = Intent(this, TimesActivity::class.java)
            startActivity(intent)
        }

        termsContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, TERMS_URL.toUri())
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        isRestoringSwitchState = true
        activateSwitch.isChecked = sharedHelper.getSwitchState()
        isRestoringSwitchState = false
        if (BuildConfig.DEBUG) Log.d(tag, "Switch state restored in onResume: ${activateSwitch.isChecked}")
        tvSwitchState.text = if (activateSwitch.isChecked) getString(R.string.On) else getString(R.string.Off)

        registerReceiver(dndStateReceiver, IntentFilter(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED))
        checkDndAndUpdateSwitch()

        if (sharedHelper.getTermsAccepted() && permissionsHelper.checkLocationPermission() && permissionsHelper.checkNotificationPermission() && permissionsHelper.checkDNDPermission(this) && !permissionsHelper.checkAlarmPermission())
            permissionsHelper.requestAlarmPermission(this)
        else if (sharedHelper.getTermsAccepted() && permissionsHelper.checkLocationPermission() && permissionsHelper.checkNotificationPermission() && permissionsHelper.checkDNDPermission(this) && permissionsHelper.checkAlarmPermission() && !permissionsHelper.checkBackgroundLocationPermission())
            permissionsHelper.requestBackgroundLocationPermission(this)
        else if (permissionsHelper.areAllPermissionsGranted() && !sharedHelper.isPermissionsSnackbarShown()) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.permissions_granted), Snackbar.LENGTH_SHORT).show()
            sharedHelper.setPermissionsSnackbarShown(true)
        }

        tvCalculationMethod.text = sharedHelper.getStringFromArray(R.array.calculation_methods, SharedHelper.SELECTED_METHOD_RES_ID, 0)
        tvSilentModeType.text = sharedHelper.getStringFromArray(R.array.silent_mode_types, SharedHelper.SILENT_MODE_TYPE, 0)
        tvBeforeDhuhr.text = sharedHelper.getStringFromArray(R.array.before_dhuhr_duration, SharedHelper.DURATION_BEFORE_DHUHR, 0)
        tvAfterDhuhr.text = sharedHelper.getStringFromArray(R.array.after_dhuhr_duration, SharedHelper.DURATION_AFTER_DHUHR, 3)
        tvTarawih.text = sharedHelper.getStringFromArray(R.array.tarawih_duration, SharedHelper.DURATION_TARAWIH, 4)
        tvTahajjud.text = sharedHelper.getStringFromArray(R.array.tahajjud_duration, SharedHelper.DURATION_TAHAJJUD, 0)
        tvEidTime.text = sharedHelper.getStringFromArray(R.array.eid_time, SharedHelper.SELECTED_TIME_EID, 0)
        tvEidDuration.text = sharedHelper.getStringFromArray(R.array.eid_duration, SharedHelper.DURATION_EID, 1)

        tvFajrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_FAJR, 2)
        tvFajrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_FAJR, 4)
        tvSunriseBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_SUNRISE, 0)
        tvSunriseAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_SUNRISE, 0)
        tvDhuhrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_DHUHR_DAILY, 2)
        tvDhuhrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_DHUHR_DAILY, 4)
        tvAsrBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ASR, 2)
        tvAsrAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ASR, 4)
        tvMaghribBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_MAGHRIB, 2)
        tvMaghribAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_MAGHRIB, 4)
        tvIshaBefore.text = sharedHelper.getStringFromArray(R.array.before_prayer_durations, SharedHelper.DURATION_BEFORE_ISHA, 2)
        tvIshaAfter.text = sharedHelper.getStringFromArray(R.array.after_prayer_durations, SharedHelper.DURATION_AFTER_ISHA, 4)
        audioSwitch.isChecked = sharedHelper.getAudioSwitchState()
        notificationSwitch.isChecked = sharedHelper.getNotificationSwitchState()
    }

    override fun onPause() {
        super.onPause()
        sharedHelper.saveSwitchState(activateSwitch.isChecked)
        sharedHelper.saveAudioSwitchState(audioSwitch.isChecked)
        sharedHelper.saveNotificationSwitchState(notificationSwitch.isChecked)
        unregisterReceiver(dndStateReceiver)
        if (BuildConfig.DEBUG) Log.d(tag, "Switch state saved in onPause: ${activateSwitch.isChecked}")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) Log.d(tag, "onDestroy accessed")
    }

    private fun showWelcomeDialog() {
        val dialog = WelcomeDialog()
        dialog.show(supportFragmentManager, "WelcomeDialog")
    }

    override fun onNextClicked() {
        if (BuildConfig.DEBUG) Log.d(tag, "Welcome dialog next clicked. Showing terms.")
        showTermsAndConditionsDialog()
    }

    private fun showTermsAndConditionsDialog() {
        val dialog = TermsAndConditions()
        dialog.show(supportFragmentManager, "TermsAndConditionsDialog")
    }

    override fun onTermsAccepted() {
        if (BuildConfig.DEBUG) Log.d(tag, "Terms accepted callback received. Proceeding with permissions.")
        requestLocationPermission()
    }

    override fun onTermsDeclined() {
        if (BuildConfig.DEBUG) Log.d(tag, "Terms declined callback received. Exiting app.")
        finish()
    }

    private fun checkDndAndUpdateSwitch() {
        if (activateSwitch.isChecked && !permissionsHelper.checkDNDPermission(this))
            runOnUiThread { switchStateOff(R.string.grant_dnd_permission) }
    }

    private fun titlePadding () {
        val mainLinearLayout = findViewById<LinearLayout>(R.id.main_linear_layout)
        ViewCompat.setOnApplyWindowInsetsListener(mainLinearLayout) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.displayCutout
            var topOffset = systemBarsInsets.top
            if (displayCutout != null) {
                val safeInsetTop = displayCutout.safeInsetTop
                if (safeInsetTop > topOffset) topOffset = safeInsetTop
            }
            val customOffsetPx = (30 * resources.displayMetrics.density).toInt()
            topOffset += customOffsetPx
            view.updatePadding(left = systemBarsInsets.left, top = topOffset, right = systemBarsInsets.right, bottom = systemBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun layoutDirection() {
        if (LocaleHelper.getPersistedLocale() == "ar" || LocaleHelper.getPersistedLocale() == "ur")
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        else window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
         else handleLocationGranted()
    }

    private fun handleLocationGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else handleNotificationGranted()
    }

    private fun handleNotificationGranted() {
        when {
            !permissionsHelper.checkDNDPermission(this) -> permissionsHelper.requestDNDPermission(this)
            !permissionsHelper.checkAlarmPermission() -> permissionsHelper.requestAlarmPermission(this)
            !permissionsHelper.checkBackgroundLocationPermission() -> permissionsHelper.requestBackgroundLocationPermission(this)
            else -> {}
        }
    }

    private fun showLocationDenied() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.location_permission_denied_title)
                .setMessage(R.string.location_permission_denied_message)
                .setPositiveButton(R.string.try_again) { _, _ -> requestLocationPermission() }
                .setNegativeButton(R.string.cancel) { _, _ -> Snackbar.make(findViewById(android.R.id.content), getString(R.string.grant_location_permission), Snackbar.LENGTH_SHORT).show() }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.location_permanently_denied_title)
                .setMessage(R.string.location_permanently_denied_message)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
                .setNegativeButton(R.string.exit) { _, _ -> finish() }
                .show()
        }
    }

    private fun showNotificationDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.notification_recommended_title)
                .setMessage(R.string.notification_recommended_message)
                .setPositiveButton(R.string.enable) { _, _ -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                .setNegativeButton(R.string.skip) { _, _ -> handleNotificationGranted() }
                .show()
        } else {
            handleNotificationGranted()
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.notification_disabled), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun handleContainerClick(container: View, titleResId: Int, optionsResId: Int, selectedIndexKey: String, defaultIndex: Int, launcher: ActivityResultLauncher<Intent>) {
        container.setOnClickListener {
            if (activateSwitch.isChecked) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.cannot_change_settings), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SelectionActivity::class.java).apply {
                putExtra("TITLE", getString(titleResId))
                putExtra("OPTIONS", resources.getStringArray(optionsResId))
                putExtra("SELECTED_INDEX_KEY", selectedIndexKey)
                putExtra("DEFAULT_INDEX", defaultIndex)
            }
            launcher.launch(intent)
        }
    }

    private fun switchOn() {
        tvSwitchState.text = getString(R.string.On)
        if (BuildConfig.DEBUG) Log.d(tag, "Main Switch is turned on")
        if (!permissionsHelper.checkLocationPermission()) {
            requestLocationPermission()
            runOnUiThread { switchStateOff(R.string.grant_location_permission) }
        } else if (!permissionsHelper.checkDNDPermission(this)) {
            permissionsHelper.requestDNDPermission(this)
            runOnUiThread { switchStateOff(R.string.grant_dnd_permission) }
        } else if (!permissionsHelper.checkAlarmPermission()) {
            permissionsHelper.requestAlarmPermission(this)
            runOnUiThread { switchStateOff(R.string.grant_alarm_permission) }
        } else if (!permissionsHelper.checkBackgroundLocationPermission()) {
            permissionsHelper.requestBackgroundLocationPermission(this)
            runOnUiThread { switchStateOff(R.string.background_location_denied_message) }
        } else {
            startLocationService()
            lifecycleScope.launch(Dispatchers.IO) {
                val selectedMethodIndex = sharedHelper.getIntValue(SharedHelper.SELECTED_METHOD_RES_ID, 0)
                if (tools.checkIfDataAvailable() && !tools.processMethodChange(selectedMethodIndex)) {
                    if (BuildConfig.DEBUG) Log.i(tag, "Prayer times data already available, processing...")
                    tools.processPrayerTimes()
                    AlarmScheduler(this@MainActivity).scheduleDailyAlarm()
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.app_enabled), Snackbar.LENGTH_SHORT).show()
                } else {
                    if (BuildConfig.DEBUG) Log.i(tag, "Prayer times data not available/obsolete or method changed, retrieving...")
                    if (tools.findLocation(selectedMethodIndex) && tools.isInternetAvailable()) {
                        sharedHelper.saveIntValue(SharedHelper.SELECTED_METHOD_RES_ID, selectedMethodIndex)
                        sharedHelper.saveLastCheckedMethodIndex(selectedMethodIndex)
                        AlarmScheduler(this@MainActivity).scheduleDailyAlarm()
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.app_enabled), Snackbar.LENGTH_SHORT).show()
                    } else {
                        Log.e(tag, "Location disabled or No Internet connexion")
                        runOnUiThread { switchStateOff(R.string.no_location_internet) }
                    }
                }
            }
        }
    }

    private fun switchOff() {
        tools.exitSilentMode()
        tools.cancelAllSilentModes()
        tools.cancelScheduledSilentMode()
        AlarmScheduler(this@MainActivity).cancelDailyAlarm()
        stopLocationService()
        tvSwitchState.text = getString(R.string.Off)
        sharedHelper.saveSwitchState(false)
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.app_disabled), Snackbar.LENGTH_SHORT).show()
        if (BuildConfig.DEBUG) Log.d(tag, "Main Switch is turned off.")
    }

    fun switchStateOff(message: Int) {
        sharedHelper.saveSwitchState(false)
        activateSwitch.isChecked = false
        tvSwitchState.text = getString(R.string.Off)
        Snackbar.make(findViewById(android.R.id.content), getString(message), Snackbar.LENGTH_SHORT).show()
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(this, serviceIntent)
        else startService(serviceIntent)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    private fun setupContainer(containerId: Int, titleResId: Int, contentResId: Int, screenType: String) {
        findViewById<MaterialCardView>(containerId).setOnClickListener {
            val intent = Intent(this, InformationActivity::class.java).apply {
                putExtra("TITLE", getString(titleResId))
                putExtra("CONTENT", getString(contentResId))
                putExtra("SCREEN_TYPE", screenType)
            }
            startActivity(intent)
        }
    }
}