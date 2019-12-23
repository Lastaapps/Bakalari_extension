package cz.lastaapps.bakalariextension.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.App
import java.lang.Double.min
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


class LoginData {

    companion object {
        private val SP_KEY = "LOGIN"
        private val SP_URL = "URL"
        private val SP_USERNAME = "USERNAME"
        private val SP_PASSWORD = "TOP_SECRET_PASSWORD"
        private val SP_TOWN = "TOWN"
        private val SP_SCHOOL = "SCHOOL"

        fun getUsername(): String { return get(SP_USERNAME) }
     private fun getPassword(): String { return get(SP_PASSWORD) }
        fun getUrl():      String { return get(SP_URL) }
        fun getTown():     String { return get(SP_TOWN) }
        fun getSchool():   String { return get(SP_SCHOOL) }

        private fun get(key: String): String {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        fun saveData(username: String, password: String, url: String, town: String, school: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(SP_USERNAME, username)
                putString(SP_PASSWORD, password)
                putString(SP_URL, url)
                putString(SP_TOWN, town)
                putString(SP_SCHOOL, school)
                apply()
            }
        }

        open fun generateToken(salt: String, ikod: String, typ: String,
                               username: String = getUsername(), password: String = getPassword()): String {
            val pwd = hash(salt + ikod + typ + password)
            println(pwd)
            val date = SimpleDateFormat("YYYYMMdd").format(Date())
            val toHash = "*login*$username*pwd*$pwd*sgn*ANDR$date"
            println(toHash)
            val hashed = hash(toHash)
            println(hashed)
            return hashed.replace('\\', '_').replace('/', '_').replace('+', '-')

        }

        private fun hash(input: String): String {
            val md = MessageDigest.getInstance("SHA-512")
            md.update(input.toByteArray(Charsets.UTF_8))
            val byteData = md.digest()

            return Base64.encodeToString(byteData, Base64.NO_WRAP).trim()

        }
    }
}