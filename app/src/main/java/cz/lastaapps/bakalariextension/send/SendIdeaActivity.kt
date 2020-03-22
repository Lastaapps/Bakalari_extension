package cz.lastaapps.bakalariextension.send

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.io.IOException

/**
 * Sends idea to Firebase database
 * Limited to 1 per day
 */
class SendIdeaActivity : AppCompatActivity() {

    companion object {
        private val TAG = SendIdeaActivity::class.java.simpleName
        private const val SP_KEY = "SEND_IDEA"
        private const val SP_DATE_KEY = "LAST_SENT"
    }

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (timeCheck()) {
            setContentView(R.layout.activity_send_idea)

            database = FirebaseDatabase.getInstance().reference

            //sends data to Firebase
            val fab = findViewById<FloatingActionButton>(R.id.idea_fab)
            fab.setOnClickListener {
                getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(SP_DATE_KEY,
                        ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli())
                    .apply()
                send()
            }
        } else {
            //If limit per day was reached
            AlertDialog.Builder(this)
                .setMessage(R.string.idea_overload)
                .setPositiveButton(R.string.idea_go_back) { dialog, _ -> run{
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
                    phoneId = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID))

                database.child("idea").child(id).setValue(obj)

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
        var phoneId: String? = ""
    )
}
