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

package cz.lastaapps.bakalariextension.api.user.data

import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.SimpleData

typealias ModuleMap = HashMap<String, ArrayList<String>>

class User(
    uid: String,
    val classInfo: SimpleData,
    val fullName: String,
    val schoolName: String,
    val schoolType: String,
    val userType: String,
    val userTypeText: String,
    val studyYear: Int,
    val modules: ModuleMap,
    val semester: Semester?
) : DataId<String>(uid) {

    companion object {
        private val TAG = User::class.java

        const val ROLE_PARENT = "parents"

        //TODO not sure about names
        const val ROLE_SYSTEM = "system"
        const val ROLE_TEACHER = "teachers"
        const val ROLE_STUDENT = "students"
        const val ROLE_HEADMASTERSHIP = "headmastership"

        //modules permissions
        const val KOMENS = "Komens"
        const val KOMENS_SHOW_RECEIVED = "ShowReceivedMessages"
        const val KOMENS_SHOW_SEND = "ShowSentMessages"
        const val KOMENS_SHOW_NOTICEBOARD = "ShowNoticeBoardMessages"
        const val KOMENS_SHOW_RATING = "ShowRatingDetails"
        const val KOMENS_SEND_MESSAGES = "SendMessages"
        const val KOMENS_SEND_ATTACHMENT = "SendAttachments"

        const val ABSENCE = "Absence"
        const val ABSENCE_SHOW = "ShowAbsence" //should be available always
        const val ABSENCE_SHOW_PERCENTAGE = "ShowAbsencePercentage"

        const val EVENTS = "Events"
        const val EVENTS_SHOW = "ShowEvents" //should be available always

        const val MARKS = "Marks"
        const val MARKS_SHOW = "ShowMarks" //should be available always
        const val MARKS_SHOW_FINAL = "ShowFinalMarks"
        const val MARKS_SHOW_PREDICT = "PredictMarks" //ignored

        const val TIMETABLE = "Timetable"
        const val TIMETABLE_SHOW = "ShowTimetable" //should be available always

        const val SUBSTITUTIONS = "Substitutions"
        const val SUBSTITUTIONS_SHOW = "ShowSubstitutions" //should be available always

        const val SUBJECTS = "Subjects"
        const val SUBJECTS_SHOW = "ShowSubjects" //should be available always
        const val SUBJECTS_SHOW_THEMES = "ShowSubjectThemes" //should be available always

        const val HOMEWORK = "Homeworks"
        const val HOMEWORK_SHOW = "ShowHomeworks" //should be available always

        const val GDPR = "Gdpr"
        const val GDPR_SHOW_OWN_CONSENTS = "ShowOwnConsents"
        const val GDPR_SHOW_CHILD_CONSENTS = "ShowChildConsents"
        const val GDPR_SHOW_COMMISSIONERS = "ShowCommissioners"

    }

    /**full name in first name last name format*/
    val normalFunName: String
        get() {
            val temp = fullName.substring(0, fullName.lastIndexOf(','))
            val spaceIndex = temp.indexOf(' ')
            return "${temp.substring(spaceIndex + 1)} ${temp.substring(0, spaceIndex)}"
        }

    /**
     * translates school role to current language
     * @return the name of role
     */
    private fun translateUserType(type: String = userType): String {
        return when (type) {
            ROLE_PARENT -> App.getString(R.string.role_parent)
            ROLE_TEACHER -> App.getString(R.string.role_teacher)
            ROLE_STUDENT -> App.getString(R.string.role_student)
            //TODO report letters to firebase to analyze
            //ROLE_LEADING -> App.getString(R.string.role_leading)
            ROLE_SYSTEM -> App.getString(R.string.role_system)
            else -> App.getString(R.string.role_oder)
        }
    }

    /**@return string in format 1.A first name last name*/
    fun getClassAndRole(): String {
        return "${classInfo.name} - ${translateUserType()}"
    }

    /**if whole module is enabled, for example komens, timetable, ...*/
    fun isModuleEnabled(moduleName: String): Boolean {
        return modules.keys.contains(moduleName)
    }

    /**@return in exact feature is available, for example showMarks, showFinalMarks, ...*/
    private lateinit var _allFeatures: ArrayList<String>
    fun isFeatureEnabled(featureType: String): Boolean {
        if (!this::_allFeatures.isInitialized) {
            _allFeatures = ArrayList()
            for (key in modules.keys) {
                _allFeatures.addAll(modules[key]!!)
            }
        }
        return _allFeatures.contains(featureType)
    }
}
