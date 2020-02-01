package cz.lastaapps.bakalariextension.send

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import cz.lastaapps.bakalariextension.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SendIdeaActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_idea)

        database = FirebaseDatabase.getInstance().reference

        val fab = findViewById<FloatingActionButton>(R.id.send_fab)
        fab.setOnClickListener {
            send()
        }

    }

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

    @IgnoreExtraProperties
    data class Message(
        var date: String? = "",
        var messageId: String? = "",
        var email: String? = "",
        var message: String? = "",
        var phoneId: String? = ""
    )
}
