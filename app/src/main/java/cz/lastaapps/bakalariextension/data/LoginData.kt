package cz.lastaapps.bakalariextension.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.ui.login.LoginToServer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*


class LoginData {

    companion object {
        private val SP_KEY = "LOGIN"
        private val SP_URL = "URL"
        private val SP_USERNAME = "USERNAME"
        private val SP_PASSWORD = "TOP_SECRET_PASSWORD"
        private val SP_TOWN = "TOWN"
        private val SP_SCHOOL = "SCHOOL"
        private val SP_TOKEN = "TOKEN"


        fun getUsername(): String {
            return get(SP_USERNAME)
        }

        fun getPassword(): String {
            return get(SP_PASSWORD)
        }

        fun getUrl(): String {
            return get(SP_URL)
        }

        fun getTown(): String {
            return get(SP_TOWN)
        }

        fun getSchool(): String {
            return get(SP_SCHOOL)
        }

        private fun get(key: String): String {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        fun saveData(
            username: String,
            password: String,
            url: String,
            town: String,
            school: String
        ) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(SP_USERNAME, username)
                putString(SP_PASSWORD, password)
                putString(SP_URL, url)
                putString(SP_TOWN, town)
                putString(SP_SCHOOL, school)
                apply()
            }
        }


        fun saveToken(token: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(SP_TOKEN, token)
                apply()
            }
        }

        fun getToken(lookup: Boolean = true): String {
            val token: String = get(SP_TOKEN)
            if (!check(token)) {
                if (lookup) {
                    LoginToServer().execute(getUsername(), getPassword(), getUrl(), getTown(), getSchool(), null)
                    return getToken(false)
                }
            } else
                return token
            return ""
        }


        fun check(token: String): Boolean {
            if (token == "") return false

            val stringUrl = "${getUrl()}?hx=$token&pm=login"
            println(stringUrl)
            val url = URL(stringUrl)
            val urlConnection = url.openConnection()
            val input = urlConnection.getInputStream()

            val read = BufferedReader(InputStreamReader(input)).readLine()

            return !read.contains("-1")
        }

    }
}