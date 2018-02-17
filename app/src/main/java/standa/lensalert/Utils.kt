package standa.lensalert

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

fun PreferencesManager.lensesWornOut(): Boolean {
    return this.progress >= this.duration * 10
}

fun Long.isAfterYesterday(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = System.currentTimeMillis()
    calendar.set(Calendar.HOUR, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.AM_PM, Calendar.AM)

    return this >= calendar.timeInMillis
}

fun Double.toNiceString(): String {
    return if (this % 1.0 != 0.0)
        String.format("%s", this)
    else
        String.format("%.0f", this)
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

fun String.hash64bit(): Long {
    val algorithm = "PBKDF2WithHmacSHA1"

    fun ByteArray.toLong(): Long {
        return ByteBuffer.wrap(this).long
    }

    val time = System.nanoTime()

    val spec = PBEKeySpec(this.toCharArray(), byteArrayOf(0), 256, 64)
    val f = SecretKeyFactory.getInstance(algorithm)
    val hash = f.generateSecret(spec).encoded
    Log.i("Utils", "delay: ${((System.nanoTime() - time) / 1e6).toInt()}ms")
    return hash.toLong()
}

fun Long.getStringDate(): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this

    return "${calendar[Calendar.DAY_OF_MONTH]}. ${calendar[Calendar.MONTH] + 1}. ${calendar[Calendar.YEAR]}"
}

data class TempPreferences(var progress: Int? = null,
                           var duration: Int? = null,
                           var date: Long? = null,
                           var hours: Int? = null,
                           var minutes: Int? = null,
                           var setAlarm: Boolean? = null,
                           var startDate: Long? = null) {

    fun saveChangedPreferences(preferences: PreferencesManager) {
        progress?.let { preferences.progress = it }
        duration?.let { preferences.duration = it }
        date?.let { preferences.date = it }
        hours?.let { preferences.hours = it }
        minutes?.let { preferences.minutes = it }
        setAlarm?.let { preferences.setAlarm = it }
        startDate?.let { preferences.startDate = it }

        if (preferences.lensesWornOut())
            preferences.progress = preferences.duration * 10
    }
}