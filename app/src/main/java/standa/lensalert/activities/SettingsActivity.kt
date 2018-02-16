package standa.lensalert.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_settings.*
import standa.lensalert.*
import standa.lensalert.fragments.TimePickerFragment
import standa.lensalert.receivers.AlarmSetReceiver


class SettingsActivity : AppCompatActivity(), TimePickerFragment.Handler {

    private val preferences by lazy {
        PreferencesManager(this)
    }

    private val googleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .build()
        GoogleSignIn.getClient(this, gso)
    }

    private var account: GoogleSignInAccount? = null

    private val tempPreferences = TempPreferences()

    override val hours: Int
        get() = tempPreferences.hours ?: preferences.hours
    override val minutes: Int
        get() = tempPreferences.minutes ?: preferences.minutes

    private val resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        account = GoogleSignIn.getLastSignedInAccount(this)
        updateSignButtons()

        title = "${getString(R.string.APP_NAME)} ${getString(R.string.SETTINGS)}"

        pickTimeBtn.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.show(fragmentManager, "timePicker")
        }

        saveBtn.setOnClickListener {
            try{
                val durNumber =  durNumEditText.text.toString().toInt()
                tempPreferences.duration = durNumber
            }catch (e: NumberFormatException){} //handle ""

            saveSettings()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        signInBtn.setSize(SignInButton.SIZE_WIDE)
        signInBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        signOutBtn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener(this) {
                        account = null
                        updateSignButtons()
                    }
        }

        durNumEditText.setText(preferences.duration.toString(), TextView.BufferType.EDITABLE)

        alarmCheckBox.isChecked = preferences.setAlarm
        pickTimeBtn.isEnabled = alarmCheckBox.isChecked
        alarmCheckBox.setOnCheckedChangeListener { _, isChecked ->
            pickTimeBtn.isEnabled = isChecked
            tempPreferences.setAlarm = isChecked
        }
        setTimeTextView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task?.let { handleSignInResult(it) }
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            this.account = account
            updateSignButtons()
            Log.i("SettingsActivity", "signInResult:successful id= ${account.id}")
        } catch (e: ApiException) {
            Log.w("SettingsActivity", "signInResult:failed code= ${e.statusCode}, message= ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}")
        }
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        tempPreferences.hours = hourOfDay
        tempPreferences.minutes = minute

        setTimeTextView()
    }

    private fun saveSettings(){
        tempPreferences.saveChangedPreferences(preferences)

        if(tempPreferences.setAlarm != null || tempPreferences.hours != null){
            val alarmAction =
                    if (preferences.setAlarm) AlarmSetReceiver.ACTION_ALARM_SET
                    else AlarmSetReceiver.ACTION_ALARM_DISABLE
            sendBroadcast(Intent(alarmAction))
        }

        if (preferences.lensesWornOut())
            preferences.progress = preferences.duration * 10

        val task = SyncPreferencesTask(SyncPreferencesTask.getBasicHandler(this, preferences))
        task.execute()
    }

    private fun updateSignButtons(){
        if(account == null){
            signInBtn.visibility = View.VISIBLE
            signOutBtn.visibility = View.GONE
            signOutLabel.visibility = View.GONE
        }else{
            signInBtn.visibility = View.GONE
            signOutBtn.visibility = View.VISIBLE
            signOutLabel.visibility = View.VISIBLE
            signOutLabel.text = getString(R.string.SIGNED_IN_EMAIL, account!!.email)
        }
    }

    private fun setTimeTextView(minute: Int = this.minutes, hour: Int = this.hours){
        val minuteString = if(minute.toString().length == 2) minute.toString()
                     else "0" + minute.toString()

        val hourString = if(hour.toString().length == 2) hour.toString()
                   else "0" + hour.toString()

        timeTextView.text = "$hourString:$minuteString"
    }

    companion object {
        const val RC_SIGN_IN = 4864
    }
}
