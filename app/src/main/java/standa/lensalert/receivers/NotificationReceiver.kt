package standa.lensalert.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import standa.lensalert.NotificationFactory
import standa.lensalert.PreferencesManager
import standa.lensalert.isAfterYesterday
import standa.lensalert.lensesWornOut

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("NotificationReceiver", "run")

        val preferences = PreferencesManager(context)
        if(intent.action == ACTION_SEND_NOTIFICATION){

            val notificationHandler = NotificationFactory(context)
            val notification =  if(!preferences.lensesWornOut() && !preferences.date.isAfterYesterday()) notificationHandler.promptNotification
                                else if(preferences.lensesWornOut()) notificationHandler.wornLensesNotificationHandler
                                else null

            if(notification != null)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .notify(NotificationFactory.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_SEND_NOTIFICATION = "standa.lensalert.receivers.ACTION_SEND_NOTIFICATION"
    }
}