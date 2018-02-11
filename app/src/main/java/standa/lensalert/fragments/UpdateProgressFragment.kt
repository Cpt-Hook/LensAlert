package standa.lensalert.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import standa.lensalert.R

class UpdateProgressFragment : DialogFragment(), View.OnClickListener {

    override fun onClick(view: View) {
        (activity as? Handler)?.onUpdateProgressFragmentClick(view)
        activity.fragmentManager.beginTransaction().remove(this).commit()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater: LayoutInflater = activity.layoutInflater
        val builder = AlertDialog.Builder(activity)

        val view = inflater.inflate(R.layout.fragment_update_progress, null)
        view.findViewById<Button>(R.id.yesBtn).setOnClickListener(this)
        view.findViewById<Button>(R.id.halfBtn).setOnClickListener(this)
        view.findViewById<Button>(R.id.noBtn).setOnClickListener(this)

        builder.setView(view)
        return builder.create()
    }

    interface Handler {
        fun onUpdateProgressFragmentClick(view: View)
    }
}