package standa.lensalert.activities

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import standa.lensalert.PreferencesManager
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_LENS_DURATION
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_NOTIFICATION_HOUR_KEY
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_NOTIFICATION_MINUTE_KEY
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_SET_ALARM_KEY
import standa.lensalert.R
import standa.lensalert.receivers.AlarmSetReceiver
import standa.lensalert.receivers.NotificationReceiver

class MainActivity : AppCompatActivity() {

    private val preferences by lazy {
        PreferencesManager(this)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_UI) {
                setProgressBar(preferences.progress)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                IntentFilter(ACTION_UPDATE_UI)
        )
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setProgressBar(preferences.progress)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.notificationItem -> {
                sendBroadcast(Intent(NotificationReceiver.ACTION_SEND_NOTIFICATION))
            }
            R.id.settingsItem -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(settingsIntent, 0)
            }
            R.id.aboutItem -> {
                //TODO
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        if (resultCode == Activity.RESULT_OK && dataIntent != null) {
            extractAndSaveSettings(dataIntent)
            Toast.makeText(this, getString(R.string.SETTINGS_SAVED), Toast.LENGTH_LONG).show()
        }
    }

    private fun extractAndSaveSettings(dataIntent: Intent) {
        if (dataIntent.hasExtra(PREFERENCES_NOTIFICATION_HOUR_KEY) &&
                dataIntent.hasExtra(PREFERENCES_NOTIFICATION_MINUTE_KEY)) {
            preferences.hours = dataIntent.getIntExtra(PREFERENCES_NOTIFICATION_HOUR_KEY, 0)
            preferences.minutes = dataIntent.getIntExtra(PREFERENCES_NOTIFICATION_MINUTE_KEY, 0)
        }

        if (dataIntent.hasExtra(PREFERENCES_LENS_DURATION))
            preferences.lensDuration = dataIntent.getIntExtra(PREFERENCES_LENS_DURATION, 14)

        if (dataIntent.hasExtra(PREFERENCES_SET_ALARM_KEY))
            preferences.setAlarm = dataIntent.getBooleanExtra(PREFERENCES_SET_ALARM_KEY, true)

        //alarmSet or hours and minutes changed
        if (dataIntent.hasExtra(PREFERENCES_SET_ALARM_KEY) ||
                (dataIntent.hasExtra(PREFERENCES_NOTIFICATION_HOUR_KEY) && dataIntent.hasExtra(PREFERENCES_NOTIFICATION_MINUTE_KEY))) {

            val alarmAction = if (preferences.setAlarm) AlarmSetReceiver.ACTION_ALARM_SET
            else AlarmSetReceiver.ACTION_ALARM_DISABLE
            sendBroadcast(Intent(alarmAction))
        }

        if (preferences.progress > preferences.lensDuration * 10)
            preferences.progress = preferences.lensDuration * 10
    }

    private fun setProgressBar(progress: Int) {
        progressBar.max = preferences.lensDuration * 10
        progressBar.progress = progress
        progressTextView.text = getString(R.string.PROGRESS, (preferences.progress / 10.0).toNiceString(), preferences.lensDuration)

        if (progress >= preferences.lensDuration * 10)
            progressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.colorPrimaryDark))
        else {
            progressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.colorAccent))
        }

        progressBar.invalidate()
    }

    companion object {

        fun Double.toNiceString(): String {
            return if (this % 1.0 != 0.0)
                String.format("%s", this)
            else
                String.format("%.0f", this)
        }

        const val ACTION_UPDATE_UI = "standa.lensalert.activities.ACTION_UPDATE_UI"
    }
}