package cz.lastaapps.bakalariextension.api

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.App

/**
 * Used to read basic data about student from server
 * */
class Login {

    companion object {
        private val TAG = Login::class.java.simpleName

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

        const val ROLE_TEACHER = "U"
        const val ROLE_PARENT = "R"
        const val ROLE_PUPIL = "Z"

        fun get(key: String): String {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        fun set(key: String, value: String) {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(key, value)
                apply()
            }
        }

        fun clear() {
            App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                clear()
                apply()
            }
        }
        //end shared preferences

        /**
         * tries to load data from server
         * @return if data was loaded
         */
        fun login(): Boolean {

            val json = ConnMgr.serverGet("login", LoginData.getToken()) ?: return false
            try {

                set(VERSION, json.getString("AppVersion"))
                set(NAME, {
                    val value = json.getString("UserName")
                    val temp = value.substring(0, value.lastIndexOf(','))
                    val spaceIndex = temp.indexOf(' ')
                    "${temp.substring(spaceIndex + 1)} ${temp.substring(0, spaceIndex)}"
                }.invoke())
                set(ROLE, json.getString("UserType"))
                json.getString("UserTypeAsStr")//handled in #parseRole()
                set(SCHOOL, json.getString("SchoolName"))
                set(SCHOOL_TYPE, json.getString("SchoolType"))
                set(CLASS, json.getString("Class"))
                set(GRADE, json.getString("StudyYear"))
                set(MODULES, json.getString("Modules"))
                json.getString("MessageType")

                Log.i(TAG, "Data saved")

                    val params = json.getJSONArray("Params")
                for (i in 0 until params.length()) {
                    val item = params.getJSONObject(i)
                    when (item.getString("Name")) {
                        "newmarkdays" -> {
                            set(NEW_MARKS, item.getString("Value"))
                            set(NEW_MARKS_UPDATED, System.currentTimeMillis().toString())
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    Log.e(TAG, "JSON error ${json.getString("ErrorMessage")}")
                } catch (x: Exception) {
                    Log.e(TAG, "Unknown error")
                }
                return false
            }
        }

        /**
         * translates school role to current language
         * @return the name of role
         */
        fun parseRole(role: String = get(ROLE)): String {
            return when (role) {
                ROLE_PARENT -> App.appContext().getString(R.string.parent)
                ROLE_TEACHER -> App.appContext().getString(R.string.teacher)
                ROLE_PUPIL -> App.appContext().getString(R.string.pupil)
                else -> App.appContext().getString(R.string.oder)
            }
        }

        fun getClassAndRole(): String {
            return "${get(CLASS)} - ${parseRole(get(ROLE))}"
        }
    }
}