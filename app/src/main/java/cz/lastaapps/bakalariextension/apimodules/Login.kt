package cz.lastaapps.bakalariextension.apimodules

import android.content.Context
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.data.LoginData
import cz.lastaapps.bakalariextension.ui.login.LoginActivity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.lang.Exception
import java.net.URL

class Login {

    companion object {
        private val SP_KEY = "LOGIN_API"
        val VERSION = "verze"
        val NAME = "jmeno"
        val ROLE = "typ"
        val SCHOOL = "skola"
        val SCHOOL_TYPE = "typskoly"
        val CLASS = "trida"
        val GRADE = "rocnik"
        val MODULES = "moduly"
        val NEW_MARKS = "newmarkdays"
        val NEW_MARKS_UPDATED = "newmarksupdated"

        fun get(key: String): String {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).getString(key, "").toString()
        }

        private fun save(key: String, value: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit() {
                putString(key, value)
                apply()
            }
        }

        fun login(token: String) {

            try {
                val schoolUrl = LoginData.getUrl()
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
                                NAME -> save(NAME, value.substring(0, value.lastIndexOf(',')))
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
            } catch (e: Exception) {
            } finally {
            }
            println()
        }

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