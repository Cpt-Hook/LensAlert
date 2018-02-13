package standa.lensalert.activities

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import standa.lensalert.*
import standa.lensalert.fragments.PromptFragment
import standa.lensalert.fragments.NumberPickerFragment
import standa.lensalert.fragments.UpdateProgressFragment
import standa.lensalert.services.ProgressSaverService
import standa.lensalert.services.ProgressSaverService.Companion.PREFERENCES_HALF
import standa.lensalert.services.ProgressSaverService.Companion.PREFERENCES_NO
import standa.lensalert.services.ProgressSaverService.Companion.PREFERENCES_YES

class MainActivity : AppCompatActivity(), PromptFragment.Handler, UpdateProgressFragment.Handler, NumberPickerFragment.Handler {

    override val preferences by lazy {
        PreferencesManager(this)
    }

    private var lastSynced: Long? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_UI) {
                updateUI()
            }
        }
    }

    private val getPreferencesResultHandler = object: ResultHandler {
        override val context: Context
            get() = this@MainActivity
        override val preferences: PreferencesManager
            get() = this@MainActivity.preferences
        override val preExecute: () -> Unit = {
            loadingProgressBar.visibility = View.VISIBLE
        }
        override val postExecute: (Int)-> Unit = {
            loadingProgressBar.visibility = View.INVISIBLE
            updateUI()
            lastSynced = System.currentTimeMillis()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if(savedInstanceState != null && savedInstanceState.containsKey(LAST_SYNCED_KEY))
            lastSynced = savedInstanceState.getLong(LAST_SYNCED_KEY)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setBtn.setOnClickListener {
            val progressFragment = UpdateProgressFragment()
            progressFragment.show(fragmentManager, "updateProgress")
        }

        clearBtn.setOnClickListener {
            val prompt = PromptFragment()
            val arguments = Bundle(4)
            arguments.putInt(PromptFragment.ID, CLEAR_PROMPT_ID)
            arguments.putString(PromptFragment.MESSAGE, getString(R.string.CLEAR_PROMPT))
            prompt.arguments = arguments

            prompt.show(fragmentManager, "clearPrompt")
        }

        chooseBtn.setOnClickListener {
            val progressFragment = NumberPickerFragment()
            progressFragment.show(fragmentManager, "pickNumber")
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                IntentFilter(ACTION_UPDATE_UI)
        )
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        lastSynced?.let {
            if(it + (1000 * 60 * 30) < System.currentTimeMillis()){
                val task = SyncPreferencesTask(getPreferencesResultHandler)
                task.execute()
                return
            }
        }
        if(lastSynced == null){
            val task = SyncPreferencesTask(getPreferencesResultHandler)
            task.execute()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        lastSynced?.let { outState?.putLong(LAST_SYNCED_KEY, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.refreshItem -> {
                val task = SyncPreferencesTask(getPreferencesResultHandler)
                task.execute()
            }
            R.id.settingsItem -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(settingsIntent, 0)
            }
            R.id.infoItem -> {
                //TODO
            }
            R.id.exitItem -> {
                System.exit(1)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        if (resultCode == Activity.RESULT_OK && dataIntent != null) {
            Toast.makeText(this, getString(R.string.SETTINGS_SAVED), Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUI() {
        progressBar.max = preferences.duration * 10
        progressBar.progress = preferences.progress
        progressTextView.text = getString(R.string.PROGRESS, (preferences.progress / 10.0).toNiceString(), preferences.duration)

        if (preferences.lensesWornOut())
            progressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.colorPrimaryDark))
        else {
            progressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.colorAccent))
        }

        progressBar.invalidate()

        when {
            preferences.lensesWornOut() -> {
                warningTextView.text = getString(R.string.LENSES_WORN_OUT)
                warningTextView.visibility = View.VISIBLE
            }
            preferences.date.isAfterYesterday() -> {
                warningTextView.text = getString(R.string.WARNING)
                warningTextView.visibility = View.VISIBLE
            }
            else -> warningTextView.visibility = View.GONE
        }
    }

    override fun onUpdateProgressFragmentClick(view: View) {
        val intent = Intent(applicationContext, ProgressSaverService::class.java)

        intent.putExtra(PreferencesManager.PREFERENCES_PROGRESS_KEY,
                when (view.id) {
                    R.id.yesBtn -> PREFERENCES_YES
                    R.id.halfBtn -> PREFERENCES_HALF
                    R.id.noBtn -> PREFERENCES_NO
                    else -> null
                })
        startService(intent)
    }

    override fun onPromptFragmentResponse(responseCode: Int, id: Int) {
        if(id == CLEAR_PROMPT_ID && responseCode == PromptFragment.POSITIVE){
            preferences.progress = 0
            preferences.date = 0
            val task = SyncPreferencesTask(SyncPreferencesTask.getBasicHandler(this, preferences))
            task.execute()
            updateUI()
        }
    }

    override fun onNumberPickerFragmentClick(number: Double?) {
        if(number != null){
            preferences.progress = (number*10).toInt()
            preferences.date = System.currentTimeMillis()
            val task = SyncPreferencesTask(SyncPreferencesTask.getBasicHandler(this, preferences))
            task.execute()
            updateUI()
        }
    }

    companion object {
        const val ACTION_UPDATE_UI = "standa.lensalert.activities.ACTION_UPDATE_UI"
        const val LAST_SYNCED_KEY = "LAST_SYNCED_KEY"
        private const val CLEAR_PROMPT_ID = 1
    }
}