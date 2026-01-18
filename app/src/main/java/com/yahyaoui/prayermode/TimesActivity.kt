package com.yahyaoui.prayermode

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TimesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_times)

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val dateText = findViewById<TextView>(R.id.dateText)
        val timesContainer = findViewById<LinearLayout>(R.id.timesContainer)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        dateText.text = "${getString(R.string.today_date)}: ${dateFormat.format(calendar.time)}"

        loadPrayerTimes(timesContainer)
    }

    private fun loadPrayerTimes(container: LinearLayout) {
        try {
            val file = File(filesDir, "prayer_times.txt")
            if (!file.exists()) {
                addPrayerTimeItem(container, getString(R.string.error), getString(R.string.no_data_available))
                return
            }

            val fileContent = file.bufferedReader().use { it.readText() }
            val fileJson = JSONObject(fileContent)
            val dataArray = fileJson.getJSONArray("data")

            val calendar = Calendar.getInstance()
            val gregorianDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)

            var prayerTimes: JSONObject? = null
            for (i in 0 until dataArray.length()) {
                val dayData = dataArray.getJSONObject(i)
                val gregorian = dayData.getJSONObject("date").getJSONObject("gregorian")
                val date = gregorian.getString("date")

                if (gregorianDate == date) {
                    prayerTimes = dayData.getJSONObject("timings")
                    break
                }
            }

            if (prayerTimes != null) {
                val prayerNames = listOf(
                    "Fajr" to getString(R.string.fajr_prayer_settings),
                    "Sunrise" to getString(R.string.sunrise_settings),
                    "Dhuhr" to getString(R.string.dhuhr_prayer_settings),
                    "Asr" to getString(R.string.asr_prayer_settings),
                    "Maghrib" to getString(R.string.maghrib_prayer_settings),
                    "Isha" to getString(R.string.isha_prayer_settings)
                )

                prayerNames.forEach { (key, name) ->
                    if (prayerTimes.has(key)) {
                        val time = prayerTimes.getString(key).split(" ")[0]
                        addPrayerTimeItem(container, name, time)
                    }
                }
            } else {
                addPrayerTimeItem(container, getString(R.string.error), getString(R.string.no_data_available))
            }
        } catch (e: Exception) {
            addPrayerTimeItem(container, getString(R.string.error), e.message ?: "Unknown error")
        }
    }

    private fun addPrayerTimeItem(container: LinearLayout, name: String, time: String) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.prayer_time_item, container, false)
        view.findViewById<TextView>(R.id.prayerName).text = name
        view.findViewById<TextView>(R.id.prayerTime).text = time
        container.addView(view)
    }
}
