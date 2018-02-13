package standa.lensalert

import android.content.Context
import android.net.ConnectivityManager
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

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

fun getResponse(urlString: String, params: String): String? {

    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    connection.doOutput = true

    val wr = BufferedWriter(OutputStreamWriter(connection.outputStream))
    wr.write(params)
    wr.flush()
    wr.close()

    if (connection.responseCode != 200) return null

    val reader = BufferedReader(InputStreamReader(connection.inputStream))
    val response = StringBuffer()
    reader.forEachLine {
        response.append(it)
    }

    return response.toString()
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

data class TempPreferences(var progress:Int? = null,
                           var duration: Int? = null,
                           var date: Long? = null,
                           var hours: Int? = null,
                           var minutes: Int? = null,
                           var setAlarm: Boolean? = null) {

    fun saveChangedPreferences(preferences: PreferencesManager){
        progress?.let{preferences.progress=it}
        duration?.let{preferences.duration=it}
        date?.let{preferences.date=it}
        hours?.let{preferences.hours=it}
        minutes?.let{preferences.minutes=it}
        setAlarm?.let{preferences.setAlarm=it}

        if(preferences.lensesWornOut())
            preferences.progress = preferences.duration * 10
    }
}