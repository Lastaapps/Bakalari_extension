/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Bakalari extension.
 *
 *     Bakalari extension is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Bakalari extension is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Bakalari extension.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cz.lastaapps.bakalariextension.api

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R

/**
 * Used to store basic data about student from server
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
        const val ROLE_STUDENT = "students"
        const val ROLE_HEADMASTERSHIP = "headmastership"

        /**@return string from shared preferences*/
        fun get(key: String): String {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        /**sets entry in shared preferences*/
        fun set(key: String, value: String) {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(key, value)
                apply()
            }
        }

        /**clears saved data*/
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
        fun download(): Boolean {

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
                ROLE_STUDENT -> App.getString(R.string.role_student)
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