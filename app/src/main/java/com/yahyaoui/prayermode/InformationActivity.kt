package com.yahyaoui.prayermode

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import android.util.TypedValue
import android.text.method.LinkMovementMethod
import com.yahyaoui.prayermode.TermsAndConditions.Companion.PRIVACY_URL
import com.yahyaoui.prayermode.TermsAndConditions.Companion.DONATION_URL

class InformationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)
        findViewById<ImageView>(R.id.selectionBackButton).setOnClickListener { finish() }

        val title = intent.getStringExtra("TITLE") ?: "No Title"
        findViewById<TextView>(R.id.selectionTitle).text = title
        val content = intent.getStringExtra("CONTENT") ?: "No Content"
        findViewById<TextView>(R.id.selectionContent).text = content
        val screenType = intent.getStringExtra("SCREEN_TYPE") ?: "PERMISSIONS"
        val dynamicContainer = findViewById<LinearLayout>(R.id.dynamicContainer)

        when (screenType) {
            "PRIVACY" -> {
                val privacyNoticeView = TextView(this).apply {
                    val privacyPolicyUrl = PRIVACY_URL
                    val privacyPolicyLinkText = getString(R.string.privacy_policy_link_text)
                    val linkHtml = "<a href=\"$privacyPolicyUrl\">$privacyPolicyLinkText</a>"
                    val rawHtmlString = getString(R.string.privacy_description)
                    val finalHtmlString = rawHtmlString.replace("[PRIVACY_LINK]", linkHtml)
                    text = HtmlCompat.fromHtml(finalHtmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    movementMethod = LinkMovementMethod.getInstance()
                    textSize = 16f
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(48, 16, 48, 16)
                }
                dynamicContainer.addView(privacyNoticeView)
            }
            "PERMISSIONS" -> {
                val permissions = listOf(
                    PermissionItem(R.drawable.ic_location, applicationContext.getString(R.string.location), applicationContext.getString(R.string.location_description)),
                    PermissionItem(R.drawable.ic_notification, applicationContext.getString(R.string.notifications), applicationContext.getString(R.string.notifications_description)),
                    PermissionItem(R.drawable.ic_dnd, applicationContext.getString(R.string.dnd), applicationContext.getString(R.string.dnd_description)),
                    PermissionItem(R.drawable.ic_alarm, applicationContext.getString(R.string.alarm), applicationContext.getString(R.string.alarm_description))
                )
                permissions.forEach { permission ->
                    val view = LayoutInflater.from(this)
                        .inflate(R.layout.permission_item, dynamicContainer, false)
                    view.findViewById<ImageView>(R.id.icon).setImageResource(permission.iconResId)
                    view.findViewById<TextView>(R.id.title).text = permission.title
                    view.findViewById<TextView>(R.id.description).text = permission.description
                    dynamicContainer.addView(view)
                }
            }
            "HELP" -> {
                val helpItems = listOf(
                    PermissionItem(R.drawable.ic_bulb, applicationContext.getString(R.string.bulb), applicationContext.getString(R.string.bulb_description)),
                    PermissionItem(R.drawable.ic_baseline_info, applicationContext.getString(R.string.important_note), applicationContext.getString(R.string.important_note_description)),
                    PermissionItem(R.drawable.ic_volume_off, applicationContext.getString(R.string.volume), applicationContext.getString(R.string.volume_description)),
                    PermissionItem(R.drawable.ic_calculation, applicationContext.getString(R.string.calculation), applicationContext.getString(R.string.calculation_description)),
                    PermissionItem(R.drawable.ic_duration, applicationContext.getString(R.string.durations), applicationContext.getString(R.string.durations_description)),
                    PermissionItem(R.drawable.ic_time, applicationContext.getString(R.string.time), applicationContext.getString(R.string.time_description)),
                    PermissionItem(R.drawable.ic_prayer_mat_inverted, applicationContext.getString(R.string.tile), applicationContext.getString(R.string.tile_description)),
                    PermissionItem(R.drawable.ic_wearable, applicationContext.getString(R.string.wearable), applicationContext.getString(R.string.wearable_description))
                )
                helpItems.forEach { helpItem ->
                    val view = LayoutInflater.from(this)
                        .inflate(R.layout.permission_item, dynamicContainer, false)
                    view.findViewById<ImageView>(R.id.icon).setImageResource(helpItem.iconResId)
                    view.findViewById<TextView>(R.id.title).text = helpItem.title
                    view.findViewById<TextView>(R.id.description).text = helpItem.description
                    dynamicContainer.addView(view)
                }
            }
            "DONATION" -> {
                val donationView = TextView(this).apply {
                    val donationUrl = DONATION_URL
                    val donationLinkText = getString(R.string.donation_link_text)
                    val linkHtml = "<a href=\"$donationUrl\">$donationLinkText</a>"
                    val rawHtmlString = getString(R.string.donation_description)
                    val finalHtmlString = rawHtmlString.replace("[DONATION_LINK]", linkHtml)
                    text = HtmlCompat.fromHtml(finalHtmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    movementMethod = LinkMovementMethod.getInstance()
                    textSize = 16f
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(48, 16, 48, 16)
                }
                dynamicContainer.addView(donationView)
            }
        }
    }
}
data class PermissionItem(val iconResId: Int, val title: String, val description: String)