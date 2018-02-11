package standa.lensalert.fragments

import android.app.Dialog
import android.app.DialogFragment
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker

class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private val settingsActivity by lazy {
        activity as Handler
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return TimePickerDialog(activity, this,
                settingsActivity.hours, settingsActivity.minutes,true)
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        settingsActivity.onTimeSet(view, hourOfDay, minute)
    }

    interface Handler: TimePickerDialog.OnTimeSetListener {
        var hours: Int
        var minutes: Int
    }
}