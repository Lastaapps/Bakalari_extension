package cz.lastaapps.bakalariextension.send

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.io.IOException

/**
 * Sends error report to Firebase database
 * Limited to 1 per day
 */
class ReportIssueActivity : AppCompatActivity() {

    companion object {
        private val TAG = ReportIssueActivity::class.java.simpleName

        private const val SP_KEY = "REPORT_ISSUE"
        private const val SP_DATE_KEY = "LAST_SENT"
    }

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (timeCheck()) {
            setContentView(R.layout.activity_report)

            database = FirebaseDatabase.getInstance().reference

            //sends data to Firebase
            val fab = findViewById<FloatingActionButton>(R.id.report_fab)
            fab.setOnClickListener {
                getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(SP_DATE_KEY,
                        ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli())
                    .apply()
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

        val cal = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastSent), TimeTools.UTC)
        val now = TimeTools.now

        if (cal.isAfter(now))
            return false

        return cal.toLocalDate() != now.toLocalDate()
    }

    /**
     * Sends needed data to Firebase
     */
    //TODO add ability to send each modules data to debug
    private fun send() {
        val email = findViewById<EditText>(R.id.email).text.trim().toString()
        val message = findViewById<EditText>(R.id.message).text.trim().toString()

        if (message != "") {
            try {
                val id = database.push().key.toString()
                val obj = Message(
                    date = TimeTools.format(TimeTools.now, TimeTools.COMPLETE_FORMAT),
                    messageId = id,
                    email = email,
                    message = message,
                    phoneId = {
                        (LoginData.userID + LoginData.town + LoginData.school)
                            .hashCode().toString()
                    }.invoke(),
                    phoneType = getDeviceName(),
                    androidVersion = packageManager.getPackageInfo(packageName, 0).versionName,
                    appVersionCode = BuildConfig.VERSION_CODE.toString(),
                    appVersionName = BuildConfig.VERSION_NAME,
                    school = LoginData.school,
                    town = LoginData.town,
                    url = LoginData.url,
                    bakalariVersion = LoginData.apiVersion,
                    accountType = User.get(User.ROLE)
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
