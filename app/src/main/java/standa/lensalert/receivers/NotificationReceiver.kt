package standa.lensalert.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import standa.lensalert.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("NotificationReceiver", "run")
        if (intent.action == ACTION_SEND_NOTIFICATION) {
            val preferences = PreferencesManager(context)
            var updated = false

            val doUpdate: (Int) -> Unit = {
                if (!updated) {
                    updated = true
                    val notificationHandler = NotificationFactory(context)
                    val notification = if (!preferences.lensesWornOut() && !preferences.date.isAfterYesterday()) notificationHandler.promptNotification
                    else if (preferences.lensesWornOut()) notificationHandler.wornLensesNotificationHandler
                    else null
                    if (notification != null)
                        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                                .notify(NotificationFactory.NOTIFICATION_ID, notification)
                }
            }

            val task = SyncPreferencesTask(SyncPreferencesTask.getBasicHandler(context, preferences, postExecute = doUpdate))
            task.execute()
        }
    }


    companion object {
        const val ACTION_SEND_NOTIFICATION = "standa.lensalert.receivers.ACTION_SEND_NOTIFICATION"
    }
}