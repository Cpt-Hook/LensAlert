package standa.lensalert

import android.content.Context
import android.os.AsyncTask
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.io.IOException


private const val URL_GET_PREFERENCES = "http://cpthook.maweb.eu/lensAlert/getPreferences.php"
private const val URL_SET_PREFERENCES = "http://cpthook.maweb.eu/lensAlert/setPreferences.php"

class SyncPreferencesTask(handler: ResultHandler) : AsyncTask<Void, Void, Int>(), ResultHandler by handler {

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

    override fun doInBackground(vararg params: Void?): Int {
        Log.i("SyncPreferencesTask", "run")

        if (!isNetworkAvailable(context)) return NO_INTERNET
        else if(account == null) return NO_ACCOUNT

        val jsonPreferences = getPreferencesJson()
        val (lastChanged, tempPreferences) = jsonPreferences?.let { getTempPreferences(it) }
                ?: return BAD_RESPONSE

        Log.i("SyncPreferencesTask", "Local time: ${preferences.lastChanged}, Server time: $lastChanged")
        return when {
            preferences.lastChanged == lastChanged -> {
                Log.i("SyncPreferencesTask", "Preferences already in sync")
                SUCCESS
            }
            preferences.lastChanged < lastChanged -> {
                Log.i("SyncPreferencesTask", "Updating local preferences")
                tempPreferences.saveChangedPreferences(preferences)
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
                "&lastChanged=${preferences.lastChanged}"

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

    companion object {
        const val NO_INTERNET = -1
        const val BAD_RESPONSE = -2
        const val NO_ACCOUNT = -3
        const val SUCCESS = 1

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