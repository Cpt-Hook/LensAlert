package standa.lensalert

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_PROGRESS_KEY
import standa.lensalert.activities.MainActivity
import standa.lensalert.services.PREFERENCES_HALF
import standa.lensalert.services.PREFERENCES_NO
import standa.lensalert.services.PREFERENCES_YES
import standa.lensalert.services.ProgressSaverService


class NotificationFactory(private val context: Context) {

    private val mainActivityPendingIntent: PendingIntent
        get() {
            val intent = Intent(context.applicationContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    val promptNotification: Notification by lazy {
        val remoteView = RemoteViews(context.packageName, R.layout.notification_main)

        Intent(context.applicationContext, ProgressSaverService::class.java).let {
            it.action = ProgressSaverService.ACTION_UPDATE_PROGRESS
            it.putExtra(PREFERENCES_PROGRESS_KEY, PREFERENCES_YES)
            it.putExtra(ProgressSaverService.CLOSE_NOTIFICATION_KEY, true)
            val yesPendingIntent = PendingIntent.getService(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT)

            it.putExtra(PREFERENCES_PROGRESS_KEY, PREFERENCES_HALF)
            val halfPendingIntent = PendingIntent.getService(context, 1, it, PendingIntent.FLAG_UPDATE_CURRENT)

            it.putExtra(PREFERENCES_PROGRESS_KEY, PREFERENCES_NO)
            val noPendingIntent = PendingIntent.getService(context, 2, it, PendingIntent.FLAG_UPDATE_CURRENT)

            remoteView.setOnClickPendingIntent(R.id.yesBtn, yesPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.halfBtn, halfPendingIntent)
            remoteView.setOnClickPendingIntent(R.id.noBtn, noPendingIntent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(mainActivityPendingIntent)
                .setVibrate(longArrayOf(250, 250))
                .setContent(remoteView)

        builder.build()
    }

    val wornLensesNotificationHandler: Notification by lazy {

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.APP_NAME))
                .setContentText(context.getString(R.string.LENSES_WORN_OUT))
                .setTicker("Lenses worn out!")
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)

        builder.build()
    }

    companion object {
        const val CHANNEL_ID = "lens_alert_channel"
        const val NOTIFICATION_ID = 1
    }
}