package cz.lastaapps.bakalariextension.api

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.R

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
        fun login(token: String): Boolean {

            try {
                val json = ConnectionManager.serverGet("login", token) ?: return false

                save(VERSION, json.getString("AppVersion"))
                save(NAME, {
                    val value = json.getString("UserName")
                    val temp = value.substring(0, value.lastIndexOf(','))
                    val spaceIndex = temp.indexOf(' ')
                    "${temp.substring(spaceIndex + 1)} ${temp.substring(0, spaceIndex)}"
                }.invoke())
                save(ROLE, json.getString("UserType"))
                json.getString("UserTypeAsStr")//handled in #parseRole()
                save(SCHOOL, json.getString("SchoolName"))
                save(SCHOOL_TYPE, json.getString("SchoolType"))
                save(CLASS, json.getString("Class"))
                save(GRADE, json.getString("StudyYear"))
                save(MODULES, json.getString("Modules"))
                json.getString("MessageType")

                val params = json.getJSONArray("Params")
                println(params)
                for (i in 0 until params.length()) {
                    val item = params.getJSONObject(i)
                    when (item.getString("Name")) {
                        "newmarkdays" -> {
                            save(NEW_MARKS, item.getString("Value"))
                            save(NEW_MARKS_UPDATED, System.currentTimeMillis().toString())
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        /**
         * translates school role to current language
         * @return the name of role
         */
        fun parseRole(role: String = get(ROLE)): String {
            return when (role) {
                "R" -> App.appContext().getString(R.string.parent)
                "U" -> App.appContext().getString(R.string.teacher)
                "Z" -> App.appContext().getString(R.string.pupil)
                else -> App.appContext().getString(R.string.oder)
            }
        }
    }
}