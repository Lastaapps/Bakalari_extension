package cz.lastaapps.bakalariextension.login

import android.content.Context
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.Crypto
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * Stores data needed to login
 */
class LoginData {

    companion object {
        //saving to shared preferences
        private const val SP_KEY = "LOGIN"
        const val SP_URL = "URL"
        const val SP_USERNAME = "USERNAME"
        private const val SP_PASSWORD = "DON'T_DO_THAT!"
        const val SP_TOWN = "TOWN"
        const val SP_SCHOOL = "SCHOOL"
        private const val SP_TOKEN = "TOKEN"

        fun get(key: String): String {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        fun set(key: String, value: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .edit().putString(key, value).apply()
        }
        /**
         * Saves all at once
         */
        fun saveData(
            username: String,
            password: String,
            url: String,
            town: String,
            school: String
        ) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(SP_USERNAME, username)
                putString(SP_PASSWORD, Crypto.encrypt(password))
                putString(SP_URL, url)
                putString(SP_TOWN, town)
                putString(SP_SCHOOL, school)
                apply()
            }
        }

        /**
         * encrypts and saves password
         */
        internal fun savePassword(pass: String) {
            set(
                SP_PASSWORD,
                Crypto.encrypt(pass)
            )
        }

        /**
         * clears password
         */
        internal fun clearPassword() {
            set(
                SP_PASSWORD,
                ""
            )
        }

        /**
         * @return decrypted password
         */
        internal fun getPassword(): String {
            return Crypto.decrypt(
                get(
                    SP_PASSWORD
                )
            )
        }


        fun saveToken(token: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(SP_TOKEN, token)
                apply()
            }
        }

        fun getToken(): String {
            return get(
                SP_TOKEN
            )
        }
        //end shared preferences

        /**
         * @return true if server responded with positive code, otherwise false
         */
        fun check(token: String): Boolean {
            if (token == "") return false

            val stringUrl = "${get(
                SP_URL
            )}?hx=$token&pm=login"
            val url = URL(stringUrl)
            val urlConnection = url.openConnection()
            val input = urlConnection.getInputStream()

            val read = BufferedReader(InputStreamReader(input)).readLine()

            return !read.contains("-1")
        }

    }
}