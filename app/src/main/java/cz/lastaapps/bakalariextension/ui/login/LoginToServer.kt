package cz.lastaapps.bakalariextension.ui.login

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.data.LoginData
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.lang.Exception
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LoginToServer : AsyncTask<Any, Unit, String>() {

    lateinit var username: String
    lateinit var password: String
    lateinit var url: String
    lateinit var town: String
    lateinit var school: String

    lateinit var salt: String
    lateinit var ikod: String
    lateinit var typ: String

    private var todoAfter: Runnable? = null

    override fun doInBackground(vararg params: Any?): String {
        try {
            val list = params[0] as ArrayList<String>
            username = list[0]
            password = list[1]
            url = list[2]
            town = list[3]
            school = list[4]

            todoAfter = if (params.isNotEmpty()) params[1] as Runnable
                     else null

            if (LoginData.getUrl() == "" || LoginData.getUsername() == "" || LoginData.getSchool() == "" || LoginData.getTown() == "")
                LoginData.saveData(username, password, url, town, school)

            when (loadSalt()) {
                false -> return ""
            }

            val token = generateToken(salt, ikod, typ, username = username, password = password)

            return if (LoginData.check(token)) token else ""
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    App.appContext(),
                    R.string.error_no_internet_or_url,
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            return ""
        }
    }

    override fun onPostExecute(token: String?) {
        Toast.makeText(
            App.appContext(), when (token != "") {
                true -> R.string.login_succeed
                else -> R.string.login_failed
            }, Toast.LENGTH_LONG
        ).show()

        if (token != null && token != "") {
            LoginData.saveData(username, password, url, town, school)
            LoginData.saveToken(token)
        }
        todoAfter?.run()
    }

    private fun generateToken(
        salt: String, ikod: String, typ: String,
        username: String = LoginData.getUsername(), password: String = LoginData.getPassword()
    ): String {
        val pwd = hash(salt + ikod + typ + password)
        val date = SimpleDateFormat("YYYYMMdd").format(Date())
        val toHash = "*login*$username*pwd*$pwd*sgn*ANDR$date"
        val hashed = hash(toHash)
        return hashed.replace('\\', '_').replace('/', '_').replace('+', '-')

    }

    private fun hash(input: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        md.update(input.toByteArray(Charsets.UTF_8))
        val byteData = md.digest()

        return Base64.encodeToString(byteData, Base64.NO_WRAP).trim()
    }

    private fun loadSalt(): Boolean {
        val url = URL("$url?gethx=$username")
        val urlConnection = url.openConnection()
        val input = urlConnection.getInputStream()

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var eventType = parser.eventType
        loop@ while (eventType != XmlPullParser.END_DOCUMENT) {
            var name: String
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> LoginActivity.townList = ArrayList()
                XmlPullParser.START_TAG -> {
                    name = parser.name
                    eventType = parser.next()
                    when (name) {
                        "res" -> when (parser.text.toInt()) {
                            2 -> return false
                        }
                        "typ" -> typ = parser.text
                        "ikod" -> ikod = parser.text
                        "salt" -> salt = parser.text
                        else -> continue@loop
                    }
                }
                XmlPullParser.TEXT -> {
                }
                XmlPullParser.END_TAG -> {
                }
            }
            eventType = parser.next()
        }
        return true
    }
}