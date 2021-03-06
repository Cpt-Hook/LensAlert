package standa.lensalert

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import standa.lensalert.receivers.AlarmSetReceiver
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection


private const val URL_GET_PREFERENCES = "https://cpthook.ddns.net/lensAlert/getPreferences.php"
private const val URL_SET_PREFERENCES = "https://cpthook.ddns.net/lensAlert/setPreferences.php"

class SyncPreferencesTask(handler: ResultHandler) : AsyncTask<String, Void, Int>(), ResultHandler by handler {
//TODO authenticate id server side -> https://developers.google.com/identity/sign-in/web/backend-auth
    private val account by lazy {
        GoogleSignIn.getLastSignedInAccount(context)
    }

    override fun onPostExecute(result: Int) {
        Log.i("SyncPreferencesTask", when (result) {
            1 -> "Finished: SUCCESS code"
            -1 -> "Finished: NO_INTERNET code"
            -2 -> "Finished: BAD_RESPONSE code"
            -3 -> "Finished: NO_ACCOUNT"
            else -> "Finished: unknown code"
        })
        postExecute?.invoke(result)
    }

    override fun onPreExecute() {
        preExecute?.invoke()
    }

    override fun doInBackground(vararg params: String?): Int {
        if (!isNetworkAvailable(context)) return NO_INTERNET
        else if (account == null) return NO_ACCOUNT

        val jsonPreferences = getPreferencesJson()
        val (lastChanged, tempPreferences) = jsonPreferences?.let { getTempPreferences(it) }
                ?: return BAD_RESPONSE

        Log.i("SyncPreferencesTask", "Local time: ${preferences.lastChanged}, Server time: $lastChanged")
        return when {
            (params.isNotEmpty() && params[0] == FORCE_UPDATE_LOCAL_PREFERENCES) || preferences.lastChanged < lastChanged -> {
                Log.i("SyncPreferencesTask", "Updating local preferences")

                if(tempPreferences.setAlarm != preferences.setAlarm ||
                        tempPreferences.hours != preferences.hours ||
                        tempPreferences.minutes != preferences.minutes){
                    tempPreferences.setAlarm?.let {
                        val alarmAction =
                                if (it) AlarmSetReceiver.ACTION_ALARM_SET
                                else AlarmSetReceiver.ACTION_ALARM_DISABLE
                        context.sendBroadcast(Intent(alarmAction))
                    }
                }

                tempPreferences.saveChangedPreferences(preferences)
                SUCCESS
            }
            preferences.lastChanged == lastChanged -> {
                Log.i("SyncPreferencesTask", "Preferences already in sync")
                SUCCESS
            }
            else -> {
                Log.i("SyncPreferencesTask", "Updating server preferences")
                sendUpdateRequest()
            }
        }
    }

    private fun sendUpdateRequest(): Int {
        val queryParams = "id=${account!!.id}" +
                "&progress=${preferences.progress}" +
                "&duration=${preferences.duration}" +
                "&hours=${preferences.hours}" +
                "&minutes=${preferences.minutes}" +
                "&date=${preferences.date}" +
                "&setAlarm=${if (preferences.setAlarm) 1 else 0}" +
                "&lastChanged=${preferences.lastChanged}" +
                "&startDate=${preferences.startDate}"

        Log.i("SyncPreferencesTask", "Querying server with params: $queryParams")

        val time = System.nanoTime()
        val response = getResponse(URL_SET_PREFERENCES, queryParams)
        Log.i("SyncPreferencesTask", "Server response: $response, delay: ${((System.nanoTime() - time) / 1e6).toInt()}ms")

        return if (response == null || !response.contains("success")) BAD_RESPONSE
        else SUCCESS
    }

    private fun getPreferencesJson(): String? {
        val queryParams = "id=${account!!.id}"
        Log.i("SyncPreferencesTask", "Querying server with params: $queryParams")

        val time = System.nanoTime()
        val response = getResponse(URL_GET_PREFERENCES, queryParams)
        Log.i("SyncPreferencesTask", "Server response: $response, delay: ${((System.nanoTime() - time) / 1e6).toInt()}ms")
        return response
    }

    private fun getTempPreferences(response: String): Pair<Long, TempPreferences>? {
        try {
            val reader = JsonReader(response.reader())
            val tempPreferences = TempPreferences()
            var lastChanged: Long = 0

            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue()
                    continue
                }
                when (name) {
                    "progress" -> {
                        tempPreferences.progress = reader.nextInt()
                    }
                    "date" -> {
                        tempPreferences.date = reader.nextLong()
                    }
                    "hours" -> {
                        tempPreferences.hours = reader.nextInt()
                    }
                    "minutes" -> {
                        tempPreferences.minutes = reader.nextInt()
                    }
                    "duration" -> {
                        tempPreferences.duration = reader.nextInt()
                    }
                    "setAlarm" -> {
                        tempPreferences.setAlarm = reader.nextInt() == 1
                    }
                    "startDate" -> {
                        tempPreferences.startDate = reader.nextLong()
                    }
                    "lastChanged" -> {
                        lastChanged = reader.nextLong()
                    }
                    "status" -> {
                        if (reader.nextString() != "success") return null
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            reader.close()
            return lastChanged to tempPreferences
        } catch (e: IllegalArgumentException) {
            Log.e("SyncPreferencesTask", Log.getStackTraceString(e))
            return null
        } catch (e: IOException) {
            Log.e("SyncPreferencesTask", Log.getStackTraceString(e))
            return null
        }
    }

    private fun getResponse(urlString: String, params: String): String? {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpsURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true
        connection.connectTimeout = 5000

        try{
            connection.connect()
        }
        catch (e: IOException) {
            return null
        }

        val wr = BufferedWriter(OutputStreamWriter(connection.outputStream))
        wr.write(params)
        wr.flush()
        wr.close()

        if (connection.responseCode != 200) return null

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        reader.forEachLine {
            response.append(it)
        }

        return response.toString()
    }

    companion object {
        const val NO_INTERNET = -1
        const val BAD_RESPONSE = -2
        const val NO_ACCOUNT = -3
        const val SUCCESS = 1

        const val FORCE_UPDATE_LOCAL_PREFERENCES = "FORCE_UPDATE_LOCAL_PREFERENCES"

        fun getBasicHandler(context: Context, preferences: PreferencesManager,
                            preExecute: (() -> Unit)? = null, postExecute: ((Int) -> Unit)? = null): ResultHandler {
            return object : ResultHandler {
                override val context = context
                override val preferences = preferences
                override val postExecute = postExecute
                override val preExecute = preExecute
            }
        }
    }
}

interface ResultHandler {
    val context: Context
    val preferences: PreferencesManager
    val preExecute: (() -> Unit)?
    val postExecute: ((Int) -> Unit)?
}