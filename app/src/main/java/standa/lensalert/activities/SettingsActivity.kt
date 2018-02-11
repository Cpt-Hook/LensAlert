package standa.lensalert.activities

import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.TimePicker
import kotlinx.android.synthetic.main.activity_settings.*
import standa.lensalert.PreferencesManager
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_LENS_DURATION
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_NOTIFICATION_HOUR_KEY
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_NOTIFICATION_MINUTE_KEY
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_SET_ALARM_KEY
import standa.lensalert.R
import standa.lensalert.fragments.TimePickerFragment


class SettingsActivity : AppCompatActivity(), TimePickerFragment.Handler {

    private val preferences by lazy {
        PreferencesManager(this)
    }

    override var hours = 0
    override var minutes = 0

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
            //handle ""
            try{
                val durNumber =  durNumEditText.text.toString().toInt()
                resultIntent.putExtra(PREFERENCES_LENS_DURATION, durNumber)
            }catch (e: NumberFormatException){}

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        hours = preferences.hours
        minutes = preferences.minutes
        durNumEditText.setText(preferences.lensDuration.toString(), TextView.BufferType.EDITABLE)

        alarmCheckBox.isChecked = preferences.setAlarm
        alarmCheckBox.setOnCheckedChangeListener { _, isChecked ->
            pickTimeBtn.isEnabled = isChecked
            resultIntent.putExtra(PREFERENCES_SET_ALARM_KEY, isChecked)
        }
        pickTimeBtn.isEnabled = alarmCheckBox.isChecked
        setTimeTextView()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        resultIntent.putExtra(PREFERENCES_NOTIFICATION_HOUR_KEY, hourOfDay)
        resultIntent.putExtra(PREFERENCES_NOTIFICATION_MINUTE_KEY, minute)

        this.minutes = minute
        this.hours = hourOfDay

        setTimeTextView()
    }

    private fun setTimeTextView(minute: Int = this.minutes, hour: Int = this.hours){
        val minuteString = if(minute.toString().length == 2) minute.toString()
                     else "0" + minute.toString()

        val hourString = if(hour.toString().length == 2) hour.toString()
                   else "0" + hour.toString()

        timeTextView.text = "$hourString:$minuteString"
    }
}
