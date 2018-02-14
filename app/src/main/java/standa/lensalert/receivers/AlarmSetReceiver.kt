package standa.lensalert.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import standa.lensalert.PreferencesManager
import java.util.*

class AlarmSetReceiver : BroadcastReceiver() {

    private lateinit var preferences: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AlarmSetReceiver", "run")

        preferences = PreferencesManager(context)

        val actionIntent = Intent(NotificationReceiver.ACTION_SEND_NOTIFICATION)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        when (intent.action) {
            ACTION_ALARM_SET -> {
                setAlarm(alarmManager, pendingIntent)

            }
            ACTION_BOOT_COMPLETED -> {
                Log.i("AlarmSetReceiver", "Boot action received")
                if (preferences.setAlarm)
                    setAlarm(alarmManager, pendingIntent)
            }
            ACTION_ALARM_DISABLE -> {
                alarmManager.cancel(pendingIntent)
                Log.i("AlarmSetReceiver", "Notifications canceled")
            }
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, pendingIntent: PendingIntent) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, preferences.hours)
        calendar.set(Calendar.MINUTE, preferences.minutes)

        if(calendar.timeInMillis < System.currentTimeMillis()){
           calendar.timeInMillis += AlarmManager.INTERVAL_DAY
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, pendingIntent)

        val ampm = if(calendar[Calendar.AM_PM] == 0) "AM"
                   else "PM"
        Log.i("AlarmSetReceiver", "Notifications scheduled every ${calendar[Calendar.HOUR]}:${calendar[Calendar.MINUTE]}$ampm")
    }

    companion object {
        const val ACTION_ALARM_SET = "standa.lensalert.receivers.ACTION_ALARM_SET"
        const val ACTION_ALARM_DISABLE = "standa.lensalert.receivers.ACTION_ALARM_DISABLE"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    }
}
