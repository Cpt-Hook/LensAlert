package standa.lensalert.services

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import standa.lensalert.NotificationFactory
import standa.lensalert.PreferencesManager
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_PROGRESS_KEY
import standa.lensalert.R
import standa.lensalert.activities.MainActivity
import standa.lensalert.activities.MainActivity.Companion.lensesWornOut
import standa.lensalert.activities.MainActivity.Companion.toNiceString
import standa.lensalert.receivers.NotificationReceiver.Companion.ACTION_SEND_NOTIFICATION

class ProgressSaverService : IntentService("ProgressSaverService") {

    private val preferences by lazy {
        PreferencesManager(this)
    }

    override fun onHandleIntent(intent: Intent) {
        Log.i("ProgressSaverService", "ProgressSaverService run")

        if (intent.hasExtra(PREFERENCES_PROGRESS_KEY))
            updateProgress(intent)
    }

    private fun updateProgress(intent: Intent) {
        val toastString: String

        when (intent.getStringExtra(PREFERENCES_PROGRESS_KEY)) {
            PREFERENCES_YES -> {
                preferences.progress += 10
                toastString = getString(R.string.YES_BUTTON)
            }
            PREFERENCES_HALF -> {
                preferences.progress += 5
                toastString = getString(R.string.HALF_BUTTON)
            }
            PREFERENCES_NO -> {
                preferences.progress += 0
                toastString = getString(R.string.NO_BUTTON)
            }
            else -> {
                throw RuntimeException("Unknown action in ProgressSaverService.")
            }
        }

        if (preferences.lensesWornOut()) {
            preferences.progress = preferences.lensDuration * 10
            sendBroadcast(Intent(ACTION_SEND_NOTIFICATION))
        } else {
            if (intent.getBooleanExtra(CLOSE_NOTIFICATION_KEY, false))
                closeNotificationBar()
            makeToast("$toastString ${getString(R.string.CHOSEN)}, ${getString(R.string.PROGRESS, (preferences.progress / 10.0).toNiceString(), preferences.lensDuration)}", Toast.LENGTH_LONG)
        }

        preferences.date = System.currentTimeMillis()

        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MainActivity.ACTION_UPDATE_UI))
    }

    private fun closeNotificationBar(){
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NotificationFactory.NOTIFICATION_ID)
        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    private fun makeToast(message: String, length: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, length).show()
        }
    }

    companion object {
        const val PREFERENCES_YES = "PREFFERENCES_YES"
        const val PREFERENCES_HALF = "PREFFERENCES_HALF"
        const val PREFERENCES_NO = "PREFFERENCES_NO"
        const val CLOSE_NOTIFICATION_KEY = "CLOSE_NOTIFICATION_KEY"
    }
}