package standa.lensalert.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import standa.lensalert.NotificationFactory
import standa.lensalert.PreferencesManager

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == ACTION_SEND_NOTIFICATION){
            val preferences = PreferencesManager(context)

            val notificationHandler = NotificationFactory(context)
            val notification =  if(preferences.progress >= preferences.lensDuration *10) notificationHandler.wornLensesNotificationHandler
                                else notificationHandler.promptNotification

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NotificationFactory.NOTIFICATION_ID, notification)

            Log.i("NotificationReceiver", "NotificationReceiver run")
        }
    }

    companion object {
        const val ACTION_SEND_NOTIFICATION = "standa.lensalert.receivers.ACTION_SEND_NOTIFICATION"
    }
}