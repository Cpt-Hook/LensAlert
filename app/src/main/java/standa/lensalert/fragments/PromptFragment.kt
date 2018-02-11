package standa.lensalert.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle


class PromptFragment : DialogFragment(){

    private val responseHandler by lazy {
        activity as? Handler
    }

    private val promptId by lazy {
        arguments.getInt(ID, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setPositiveButton(arguments.getString(YES_BUTTON, "YES")) {
            _,_ -> responseHandler?.onPromptFragmentResponse(POSITIVE, promptId)
        }
        builder.setNegativeButton(arguments.getString(NO_BUTTON, "NO")) {
            _,_ -> responseHandler?.onPromptFragmentResponse(NEGATIVE, promptId)
        }
        builder.setMessage(arguments.getString(MESSAGE, "Are you sure?"))
        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        responseHandler?.onPromptFragmentResponse(CANCELED, promptId)
    }

    interface Handler {
        fun onPromptFragmentResponse(responseCode: Int, id: Int = 0)
    }

    companion object {
        const val ID = "ID"

        const val YES_BUTTON = "YES_BUTTON"
        const val NO_BUTTON = "NO_BUTTON"
        const val MESSAGE = "MESSAGE"

        const val POSITIVE = 1
        const val NEGATIVE = -1
        const val CANCELED = 0

    }
}