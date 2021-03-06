package standa.lensalert

import android.content.Context
import android.content.SharedPreferences
import standa.lensalert.PreferencesManager.Companion.PREFERENCES_LAST_CHANGED_KEY
import kotlin.reflect.KProperty

class PreferencesManager(context: Context) {

    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
    }

    val lastChanged by LongDelegate(PREFERENCES_LAST_CHANGED_KEY, 0)

    var progress by IntDelegate(PREFERENCES_PROGRESS_KEY, 0)
    var duration by IntDelegate(PREFERENCES_DURATION_KEY, 14)
    var date by LongDelegate(PREFERENCES_DATE_KEY, 0)
    var hours by IntDelegate(PREFERENCES_NOTIFICATION_HOUR_KEY, 0)
    var minutes by IntDelegate(PREFERENCES_NOTIFICATION_MINUTE_KEY, 0)
    var setAlarm by BooleanDelegate(PREFERENCES_SET_ALARM_KEY, true)
    var startDate by LongDelegate(PREFERENCES_START_DAY, -1)

    companion object {
        const val PREFERENCES_FILE_NAME = "standa.lensalert.PREFFERENCES"

        const val PREFERENCES_LAST_CHANGED_KEY = "PREFERENCES_LAST_CHANGED_KEY"

        const val PREFERENCES_PROGRESS_KEY = "PREFERENCES_PROGRESS_KEY"
        const val PREFERENCES_DURATION_KEY = "PREFERENCES_DURATION_KEY"
        const val PREFERENCES_DATE_KEY = "PREFERENCES_DATE_KEY"
        const val PREFERENCES_NOTIFICATION_HOUR_KEY = "PREFERENCES_NOTIFICATION_HOUR_KEY"
        const val PREFERENCES_NOTIFICATION_MINUTE_KEY = "PREFERENCES_NOTIFICATION_MINUTE_KEY"
        const val PREFERENCES_SET_ALARM_KEY = "PREFERENCES_SET_ALARM_KEY"
        const val PREFERENCES_START_DAY = "PREFERENCES_START_DAY"
    }
}

private class BooleanDelegate(key: String, default: Boolean): PreferenceDelegate<Boolean>(key, default) {
    override fun getValue(preferencesManager: PreferencesManager, property: KProperty<*>): Boolean {
        return preferencesManager.sharedPreferences.getBoolean(key, default)
    }

    override fun setValue(preferencesManager: PreferencesManager, property: KProperty<*>, value: Boolean) {
        preferencesManager.sharedPreferences.edit().putBoolean(key, value).putLong(PREFERENCES_LAST_CHANGED_KEY, System.currentTimeMillis()).apply()
    }
}

private class IntDelegate(key: String, default: Int): PreferenceDelegate<Int>(key, default) {
    override fun getValue(preferencesManager: PreferencesManager, property: KProperty<*>): Int {
        return preferencesManager.sharedPreferences.getInt(key, default)
    }

    override fun setValue(preferencesManager: PreferencesManager, property: KProperty<*>, value: Int) {
        preferencesManager.sharedPreferences.edit().putInt(key, value).putLong(PREFERENCES_LAST_CHANGED_KEY, System.currentTimeMillis()).apply()
    }
}

private class LongDelegate(key: String, default: Long): PreferenceDelegate<Long>(key, default) {
    override fun getValue(preferencesManager: PreferencesManager, property: KProperty<*>): Long {
        return  preferencesManager.sharedPreferences.getLong(key, default)
    }

    override fun setValue(preferencesManager: PreferencesManager, property: KProperty<*>, value: Long) {
        preferencesManager.sharedPreferences.edit().putLong(key, value).putLong(PREFERENCES_LAST_CHANGED_KEY, System.currentTimeMillis()).apply()
    }
}

private class StringDelegate(key: String, default: String): PreferenceDelegate<String>(key, default) {
    override fun getValue(preferencesManager: PreferencesManager, property: KProperty<*>): String {
        return preferencesManager.sharedPreferences.getString(key, default)
    }

    override fun setValue(preferencesManager: PreferencesManager, property: KProperty<*>, value: String) {
        preferencesManager.sharedPreferences.edit().putString(key, value).putLong(PREFERENCES_LAST_CHANGED_KEY, System.currentTimeMillis()).apply()
    }
}

private abstract class PreferenceDelegate<T>(protected val key: String, protected val default: T){
    abstract operator fun getValue(preferencesManager: PreferencesManager, property: KProperty<*>): T
    abstract operator fun setValue(preferencesManager: PreferencesManager, property: KProperty<*>, value: T)
}