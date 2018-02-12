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
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import standa.lensalert.PreferencesManager
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_LENS_DURATION
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_NOTIFICATION_HOUR_KEY
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_NOTIFICATION_MINUTE_KEY
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_SET_ALARM_KEY
import standa.lensalert.fragments.PromptFragment
import standa.lensalert.R
import standa.lensalert.fragments.NumberPickerFragment
import standa.lensalert.fragments.UpdateProgressFragment
import standa.lensalert.receivers.AlarmSetReceiver
import standa.lensalert.services.ProgressSaverService
import standa.lensalert.services.ProgressSaverService.Companion.PREFERENCES_HALF
import standa.lensalert.services.ProgressSaverService.Companion.PREFERENCES_NO
import standa.lensalert.services.ProgressSaverService.Companion.PREFERENCES_YES
import java.util.*

class MainActivity : AppCompatActivity(), PromptFragment.Handler, UpdateProgressFragment.Handler, NumberPickerFragment.Handler {

    override val preferences by lazy {
        PreferencesManager(this)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_UI) {
                updateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setBtn.setOnClickListener {
            val progressFragment = UpdateProgressFragment()
            progressFragment.show(fragmentManager, "updateProgress")
        }

        clearBtn.setOnClickListener {
            val prompt = PromptFragment()
            val arguments = Bundle(4)
            arguments.putInt(PromptFragment.ID, CLEAR_PROMPT_ID)
            arguments.putString(PromptFragment.MESSAGE, getString(R.string.CLEAR_PROMPT))
            prompt.arguments = arguments

            prompt.show(fragmentManager, "clearPrompt")
        }

        chooseBtn.setOnClickListener {
            val progressFragment = NumberPickerFragment()
            progressFragment.show(fragmentManager, "pickNumber")
        }
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
        updateUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settingsItem -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(settingsIntent, 0)
            }
            R.id.infoItem -> {
                //TODO
            }
            R.id.exitItem -> {
                System.exit(1)
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

            val alarmAction =
                    if (preferences.setAlarm) AlarmSetReceiver.ACTION_ALARM_SET
                    else AlarmSetReceiver.ACTION_ALARM_DISABLE

            sendBroadcast(Intent(alarmAction))
        }

        if (preferences.lensesWornOut())
            preferences.progress = preferences.lensDuration * 10
    }

    private fun updateUI() {
        progressBar.max = preferences.lensDuration * 10
        progressBar.progress = preferences.progress
        progressTextView.text = getString(R.string.PROGRESS, (preferences.progress / 10.0).toNiceString(), preferences.lensDuration)

        if (preferences.lensesWornOut())
            progressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.colorPrimaryDark))
        else {
            progressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.colorAccent))
        }

        progressBar.invalidate()

        when {
            preferences.lensesWornOut() -> {
                warningTextView.text = getString(R.string.LENSES_WORN_OUT)
                warningTextView.visibility = View.VISIBLE
            }
            preferences.date.isAfterYesterday() -> {
                warningTextView.text = getString(R.string.WARNING)
                warningTextView.visibility = View.VISIBLE
            }
            else -> warningTextView.visibility = View.GONE
        }
    }

    override fun onUpdateProgressFragmentClick(view: View) {
        val intent = Intent(applicationContext, ProgressSaverService::class.java)

        intent.putExtra(PreferencesManager.PREFERENCES_PROGRESS_KEY,
                when (view.id) {
                    R.id.yesBtn -> PREFERENCES_YES
                    R.id.halfBtn -> PREFERENCES_HALF
                    R.id.noBtn -> PREFERENCES_NO
                    else -> null
                })
        startService(intent)
    }

    override fun onPromptFragmentResponse(responseCode: Int, id: Int) {
        if(id == CLEAR_PROMPT_ID && responseCode == PromptFragment.POSITIVE){
            preferences.progress = 0
            preferences.date = 0
            updateUI()
        }
    }

    override fun onNumberPickerFragmentClick(number: Double?) {
        if(number != null){
            preferences.progress = (number*10).toInt()
            preferences.date = System.currentTimeMillis()
            updateUI()
        }
    }

    companion object {

        fun PreferencesManager.lensesWornOut(): Boolean {
            return this.progress >= this.lensDuration * 10
        }

        fun Long.isAfterYesterday(): Boolean {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.set(Calendar.AM_PM, Calendar.AM)

            return this >= calendar.timeInMillis
        }

        fun Double.toNiceString(): String {
            return if (this % 1.0 != 0.0)
                String.format("%s", this)
            else
                String.format("%.0f", this)
        }

        const val ACTION_UPDATE_UI = "standa.lensalert.activities.ACTION_UPDATE_UI"
        private const val CLEAR_PROMPT_ID = 1
    }
}