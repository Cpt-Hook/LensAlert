package standa.lensalert.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.TimePicker
import kotlinx.android.synthetic.main.activity_settings.*
import standa.lensalert.*
import standa.lensalert.fragments.TimePickerFragment
import standa.lensalert.receivers.AlarmSetReceiver


class SettingsActivity : AppCompatActivity(), TimePickerFragment.Handler {

    private val preferences by lazy {
        PreferencesManager(this)
    }

    private val tempPreferences = TempPreferences()

    override val hours: Int
        get() = tempPreferences.hours ?: preferences.hours
    override val minutes: Int
        get() = tempPreferences.minutes ?: preferences.minutes


    private val resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = getString(R.string.APP_NAME) + " settings"

        pickTimeBtn.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.show(fragmentManager, "timePicker")
        }

        saveBtn.setOnClickListener {
            try{
                val durNumber =  durNumEditText.text.toString().toInt()
                preferences.duration = durNumber
            }catch (e: NumberFormatException){} //handle ""

            saveSettings()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        durNumEditText.setText(preferences.duration.toString(), TextView.BufferType.EDITABLE)

        alarmCheckBox.isChecked = preferences.setAlarm
        alarmCheckBox.setOnCheckedChangeListener { _, isChecked ->
            pickTimeBtn.isEnabled = isChecked
            tempPreferences.setAlarm = isChecked
        }
        pickTimeBtn.isEnabled = alarmCheckBox.isChecked
        setTimeTextView()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        tempPreferences.hours = hourOfDay
        tempPreferences.minutes = minutes

        setTimeTextView()
    }

    private fun saveSettings(){
        tempPreferences.saveChangedPreferences(preferences)

        if(tempPreferences.setAlarm != null || tempPreferences.hours != null){
            val alarmAction =
                    if (preferences.setAlarm) AlarmSetReceiver.ACTION_ALARM_SET
                    else AlarmSetReceiver.ACTION_ALARM_DISABLE
            sendBroadcast(Intent(alarmAction))
        }

        if (preferences.lensesWornOut())
            preferences.progress = preferences.duration * 10

        val task = SyncPreferencesTask(SyncPreferencesTask.getBasicHandler(this, preferences))
        task.execute()
    }

    private fun setTimeTextView(minute: Int = this.minutes, hour: Int = this.hours){
        val minuteString = if(minute.toString().length == 2) minute.toString()
                     else "0" + minute.toString()

        val hourString = if(hour.toString().length == 2) hour.toString()
                   else "0" + hour.toString()

        timeTextView.text = "$hourString:$minuteString"
    }
}
