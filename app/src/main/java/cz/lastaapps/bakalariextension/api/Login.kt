package cz.lastaapps.bakalariextension.api

import android.content.Context
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.login.LoginActivity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.lang.Exception
import java.net.URL

/**
 * Used to read basic data about student from server
 * */
class Login {

    companion object {
        //shared preferences saving
        private const val SP_KEY = "LOGIN_API"
        const val VERSION = "verze"
        const val NAME = "jmeno"
        const val ROLE = "typ"
        const val SCHOOL = "skola"
        const val SCHOOL_TYPE = "typskoly"
        const val CLASS = "trida"
        const val GRADE = "rocnik"
        const val MODULES = "moduly"
        const val NEW_MARKS = "newmarkdays"
        const val NEW_MARKS_UPDATED = "newmarksupdated"

        fun get(key: String): String {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        private fun save(key: String, value: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit() {
                putString(key, value)
                apply()
            }
        }
        //end shared preferences

        /**
         * tries to load data from server
         * @return if data was loaded
         */
        fun login(token: String): Boolean{
            try {
                val schoolUrl = LoginData.get(
                    LoginData.SP_URL)
                val url = URL("$schoolUrl?hx=$token&pm=login")
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
                            if (eventType != XmlPullParser.TEXT) continue@loop
                            val value = parser.text.trim()

                            when (name) {
                                VERSION -> save(VERSION, value)
                                NAME -> {
                                    val temp = value.substring(0, value.lastIndexOf(','))
                                    val spaceIndex = temp.indexOf(' ')
                                    save(NAME, "${temp.substring(spaceIndex + 1)} ${temp.substring(0, spaceIndex)}")
                                }
                                ROLE -> save(ROLE, value)
                                SCHOOL -> save(SCHOOL, value)
                                SCHOOL_TYPE -> save(SCHOOL_TYPE, value)
                                CLASS -> save(CLASS, value)
                                GRADE -> save(GRADE, value)
                                MODULES -> save(MODULES, value)
                                NEW_MARKS -> {
                                    save(NEW_MARKS, value)
                                    save(NEW_MARKS_UPDATED, System.currentTimeMillis().toString())
                                }
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
            } catch (e: Exception) {
                return false
            } finally {
            }
        }

        /**
         * translates school role to current language
         * @return the name of role
         */
        fun parseRole(role: String): String {
            return when (role) {
                "R" -> App.appContext().getString(R.string.parent)
                "U" -> App.appContext().getString(R.string.teacher)
                "Z" -> App.appContext().getString(R.string.pupil)
                else -> App.appContext().getString(R.string.oder)
            }
        }
    }
}