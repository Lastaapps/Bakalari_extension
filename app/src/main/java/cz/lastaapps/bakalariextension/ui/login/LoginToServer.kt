package cz.lastaapps.bakalariextension.ui.login

import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.data.LoginData
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.URL

class LoginToServer : AsyncTask<Any, Unit, Boolean>() {

    lateinit var username: String
    lateinit var password: String
    lateinit var url: String
    lateinit var town: String
    lateinit var school: String

    lateinit var salt: String
    lateinit var ikod: String
    lateinit var typ: String

    lateinit var dialog: AlertDialog

    override fun doInBackground(vararg params: Any?): Boolean {
        try {
            val list = params[0] as ArrayList<String>
            username = list[0]
            password = list[1]
            url = list[2]
            town = list[3]
            school = list[4]

            dialog = params[1] as AlertDialog

            when (loadSalt()) {
                false -> return false
            }

            val token =
                LoginData.generateToken(salt, ikod, typ, username = username, password = password)

            return check(token)
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(App.appContext(), R.string.no_internet_or_url, Toast.LENGTH_LONG)
                    .show()
            }
            return false
        }
    }

    override fun onPostExecute(result: Boolean?) {
        Toast.makeText(App.appContext(), when(result) {
            true -> R.string.login_succeed
            else -> R.string.login_failed
        }, Toast.LENGTH_LONG).show()
        if (result!!) {
            LoginData.saveData(username, password, url, town, school)
        }
        dialog.dismiss()
    }

    private fun loadSalt(): Boolean {
        var url = URL("$url?gethx=$username")
        var urlConnection = url.openConnection()
        var input = urlConnection.getInputStream()

        var parser = XmlPullParserFactory.newInstance().newPullParser()
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

    private fun check(token: String): Boolean {
        val stringUrl = "$url?hx=$token&pm=login"
        println(stringUrl)
        val url = URL(stringUrl)
        val urlConnection = url.openConnection()
        val input = urlConnection.getInputStream()

        val read = BufferedReader(InputStreamReader(input)).readLine()

        return !read.contains("-1")
    }


}