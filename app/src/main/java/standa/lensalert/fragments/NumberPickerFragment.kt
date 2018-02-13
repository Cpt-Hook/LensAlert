package standa.lensalert.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import standa.lensalert.PreferencesManager
import standa.lensalert.R
import standa.lensalert.toNiceString

class NumberPickerFragment : DialogFragment() {

    private val handler by lazy {
        activity as Handler
    }

    private val values: Array<String> by lazy {
        Array((handler.preferences.duration * 2) + 1, { i ->
            (i/2.0).toNiceString()
        })
    }

    private var numberPicked: Double? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater: LayoutInflater = activity.layoutInflater
        val root = inflater.inflate(R.layout.fragment_number_picker, null)

        val numberPicker = root.findViewById<NumberPicker>(R.id.numberPicker)
        numberPicker.minValue = 0
        numberPicker.maxValue = values.lastIndex
        numberPicker.value = handler.preferences.progress/5
        numberPicker.displayedValues = values
        numberPicker.wrapSelectorWheel = true
        numberPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        numberPicker.setOnValueChangedListener { _, _, newVal ->
            numberPicked = values[newVal].toDouble()
        }

        val builder = AlertDialog.Builder(activity)
        builder.setView(root)
        builder.setPositiveButton(getString(R.string.OK)){
            _, _ -> handler.onNumberPickerFragmentClick(numberPicked)
        }
        builder.setNegativeButton(getString(R.string.CANCEL)){
            _, _ -> handler.onNumberPickerFragmentClick(null)
        }

        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        handler.onNumberPickerFragmentClick(null)
    }

    interface Handler {
        val preferences: PreferencesManager
        fun onNumberPickerFragmentClick(number: Double?)
    }
}