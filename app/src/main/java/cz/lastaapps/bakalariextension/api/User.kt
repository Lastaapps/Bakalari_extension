package cz.lastaapps.bakalariextension.api

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.App

/**
 * Used to read basic data about student from server
 * */
class User {

    companion object {
        private val TAG = User::class.java.simpleName

        //shared preferences saving
        private const val SP_KEY = "API_USER"
        const val UID = "uid"
        const val NAME = "fullname"
        const val ROLE = "role"
        const val SCHOOL = "school"
        const val SCHOOL_TYPE = "typskoly"
        const val CLASS_ID = "class_id"
        const val CLASS_SHORTCUT = "class_shortcut"
        const val CLASS_NAME = "class_name"
        const val GRADE = "grade"
        const val MODULES = "modules"
        const val SEMESTER_ID = "semester_id"
        const val SEMESTER_START = "semester_start"
        const val SEMESTER_END = "semester_end"

        const val ROLE_PARENT = "parents"
        //TODO not sure about names
        const val ROLE_SYSTEM = "system"
        const val ROLE_TEACHER = "teachers"
        const val ROLE_PUPIL = "pupil"
        const val ROLE_HEADQUARTERS = "headquarters"

        fun get(key: String): String {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        fun set(key: String, value: String) {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(key, value)
                apply()
            }
        }

        fun clear() {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
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

            val json = ConnMgr.serverGet("user") ?: return false
            try {

                set(UID, json.getString("UserUID"))

                val classJson = json.getJSONObject("Class")
                set(CLASS_ID, classJson.getString("Id"))
                set(CLASS_SHORTCUT, classJson.getString("Abbrev"))
                set(CLASS_NAME, classJson.getString("Name"))

                set(NAME, {
                    val value = json.getString("FullName")
                    val temp = value.substring(0, value.lastIndexOf(','))
                    val spaceIndex = temp.indexOf(' ')
                    "${temp.substring(spaceIndex + 1)} ${temp.substring(0, spaceIndex)}"
                }.invoke())
                set(SCHOOL, json.getString("SchoolOrganizationName"))
                set(SCHOOL_TYPE, json.getString("SchoolType"))
                set(ROLE, json.getString("UserType"))
                json.getString("UserTypeText")//handled in parseRole()
                set(GRADE, json.getString("StudyYear"))

                //json.getJSONArray("EnabledModules")

                val semester = json.getJSONObject("SettingModules")
                    .getJSONObject("Common")
                    .getJSONObject("ActualSemester")
                set(SEMESTER_ID, semester.getString("SemesterId"))
                set(SEMESTER_START, semester.getString("From"))
                set(SEMESTER_END, semester.getString("To"))

                Log.i(TAG, "Data saved")

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
                ROLE_PARENT -> App.getString(R.string.role_parent)
                ROLE_TEACHER -> App.getString(R.string.role_teacher)
                ROLE_PUPIL -> App.getString(R.string.role_pupil)
                //TODO report letters to firebase to analyze
                //ROLE_LEADING -> App.getString(R.string.role_leading)
                ROLE_SYSTEM -> App.getString(R.string.role_system)
                else -> App.getString(R.string.role_oder)
            }
        }

        fun getClassAndRole(): String {
            return "${get(CLASS_NAME)} - ${parseRole(get(ROLE))}"
        }
    }
}