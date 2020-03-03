package cz.lastaapps.bakalariextension.send

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import cz.lastaapps.bakalariextension.BuildConfig
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.Login
import cz.lastaapps.bakalariextension.login.LoginData
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sends error report to Firebase database
 * Limited to 1 per day
 */
class ReportIssueActivity : AppCompatActivity() {

    companion object {
        private val TAG = "${ReportIssueActivity::class.java.simpleName}"
    }

    private lateinit var database: DatabaseReference
    private val SP_KEY = "REPORT_ISSUE"
    private val SP_DATE_KEY = "LAST_SENT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (timeCheck()) {
            setContentView(R.layout.activity_report)

            database = FirebaseDatabase.getInstance().reference

            //sends data to Firebase
            val fab = findViewById<FloatingActionButton>(R.id.report_fab)
            fab.setOnClickListener {
                getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                    .edit().putLong(SP_DATE_KEY, Date().time).apply()
                send()
            }

            //Opens new issue on Github
            val githubFab = findViewById<FloatingActionButton>(R.id.github_fab)
            githubFab.setOnClickListener {
                val url = "https://github.com/Lastaapps/Bakalari_extension/issues/new"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } else {
            //If limit per day was reached
            AlertDialog.Builder(this)
                .setMessage(R.string.report_overload)
                .setPositiveButton(R.string.report_go_back) { dialog, _ -> run{
                    dialog.dismiss()
                    finish()
                } }
                .setCancelable(false)
                .create()
                .show()
        }

    }

    /**
     * @return If message was sent today, or if user is moving through time in settings
     */
    private fun timeCheck(): Boolean {
        val lastSent = getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
            .getLong(SP_DATE_KEY, 0)

        val cal = Calendar.getInstance()
        cal.time = Date(lastSent)
        val now = Calendar.getInstance()

        if (cal.after(now))
            return false

        cal.set(Calendar.HOUR, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        now.set(Calendar.HOUR, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        return cal != now
    }

    /**
     * Sends needed data to Firebase
     */
    private fun send() {
        val email = findViewById<EditText>(R.id.email).text.trim().toString()
        val message = findViewById<EditText>(R.id.message).text.trim().toString()

        if (message != "") {
            try {
                val id = database.push().key.toString()
                val obj = Message(
                    date = SimpleDateFormat("HH:mm dd.MM.YYYY z")
                        .format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time),
                    messageId = id,
                    email = email,
                    message = message,
                    phoneId = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID),
                    phoneType = getDeviceName(),
                    androidVersion = packageManager.getPackageInfo(packageName, 0).versionName,
                    appVersionCode = BuildConfig.VERSION_CODE.toString(),
                    appVersionName = BuildConfig.VERSION_NAME,
                    school = LoginData.get(
                        LoginData.SP_SCHOOL),
                    town = LoginData.get(
                        LoginData.SP_TOWN),
                    url = LoginData.get(
                        LoginData.SP_URL),
                    bakalariVersion = Login.get(Login.VERSION),
                    accountType = Login.get(Login.ROLE)
                    )
                database.child("report").child(id).setValue(obj)

                Toast.makeText(this, R.string.idea_thanks, Toast.LENGTH_LONG).show()
                finish()
            } catch (e: IOException) {
                Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, R.string.idea_empty, Toast.LENGTH_LONG).show()
        }
    }

    /**Data structure of the data to be send*/
    @IgnoreExtraProperties
    data class Message(
        var date: String? = "",
        var messageId: String? = "",
        var email: String? = "",
        var message: String? = "",
        var phoneId: String? = "",
        var phoneType: String? = "",
        var androidVersion: String? = "",
        var appVersionCode: String? = "",
        var appVersionName: String? = "",
        var school: String? = "",
        var town: String? = "",
        var url: String? = "",
        var bakalariVersion: String? = "",
        var accountType: String? = ""
    )

    private fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else capitalize(manufacturer) + " " + model
    }

    private fun capitalize(str: String): String {
        if (TextUtils.isEmpty(str)) {
            return str
        }
        val arr = str.toCharArray()
        var capitalizeNext = true
        val phrase = StringBuilder()
        for (c in arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c))
                capitalizeNext = false
                continue
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true
            }
            phrase.append(c)
        }
        return phrase.toString()
    }
}
