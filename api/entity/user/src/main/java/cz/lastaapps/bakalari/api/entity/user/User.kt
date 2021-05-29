/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalari.api.entity.user

import android.content.Context
import android.util.Base64
import cz.lastaapps.bakalari.api.entity.core.DataId
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.time.LocalDate
import java.time.Month

@Parcelize
//constructor for database
data class User(
    val uid: String,
    val campaignCategory: String,
    val classInfo: SimpleData,
    val fullName: String,
    val schoolName: String,
    val schoolType: String,
    val userType: String,
    val userTypeText: String,
    val studyYear: Int,
    val modulesFeatures: ModuleList,
    val semester: Semester?
) : DataId<String>(uid) {

    companion object {
        private val TAG = User::class.java.simpleName

        const val ROLE_PARENT = "parents"
        const val ROLE_STUDENT = "student"

        //TODO not sure about names
        const val ROLE_SYSTEM = "system"
        const val ROLE_KOMENS = "komens"
        const val ROLE_TEACHER = "teacher"
        const val ROLE_HEADMASTERSHIP = "headmastership"

        const val ABSENCE = "Absence"
        const val ABSENCE_SHOW = "ShowAbsence" //should be available always
        const val ABSENCE_SHOW_PERCENTAGE = "ShowAbsencePercentage"

        const val CAMPAIGN_SHOW = "ShowCampaign"

        const val EVENTS = "Events"
        const val EVENTS_SHOW = "ShowEvents" //should be available always

        //modules permissions
        const val KOMENS = "Komens"
        const val KOMENS_SHOW_RECEIVED = "ShowReceivedMessages"
        const val KOMENS_SHOW_SEND = "ShowSentMessages"
        const val KOMENS_SHOW_NOTICEBOARD = "ShowNoticeBoardMessages"
        const val KOMENS_SHOW_RATING = "ShowRatingDetails"
        const val KOMENS_SEND_MESSAGES = "SendMessages"
        const val KOMENS_SEND_ATTACHMENT = "SendAttachments"

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


        const val CAMPAIGN_JSON_SID = "sid"
        const val CAMPAIGN_JSON_UT = "ut"
        const val CAMPAIGN_JSON_STUDY_YEAR = "sy"
    }

    init {
        //must be sorted because of the equals method
        modulesFeatures.sort()
    }

    /**The first September of this school year*/
    @IgnoredOnParcel
    val firstSeptember: LocalDate by lazy {
        val semester = semester

        when (semester?.id) {
            1 -> {
                LocalDate.of(semester.from.year, Month.SEPTEMBER, 1)
            }
            2 -> {
                LocalDate.of(semester.from.year - 1, Month.SEPTEMBER, 1)
            }
            //computes based on current date
            else -> {
                val today = TimeTools.today.toCzechDate()
                var firstSeptemberThisYear = today
                    .withDayOfMonth(1)
                    .withMonth(9)

                //start giving new first september from August
                while (firstSeptemberThisYear > today.minusMonths(1))
                    firstSeptemberThisYear = firstSeptemberThisYear.minusYears(1)
                firstSeptemberThisYear
            }
        }
    }

    /**The last day of the school year*/
    @IgnoredOnParcel
    val lastJune: LocalDate by lazy {
        LocalDate.of(firstSeptember.year + 1, Month.JUNE, 30)
    }

    /**full name in first name last name format*/
    @IgnoredOnParcel
    val normalFullName: String by lazy {
        val temp = fullName.substring(0, fullName.lastIndexOf(','))
        val spaceIndex = temp.indexOf(' ')
        "${temp.substring(spaceIndex + 1)} ${temp.substring(0, spaceIndex)}"
    }

    /**
     * translates school role to current language
     * @return the name of role
     */
    private fun translateUserType(context: Context, type: String = userType): String {
        return when (type) {
            ROLE_PARENT -> context.getString(R.string.role_parent)
            ROLE_TEACHER -> context.getString(R.string.role_teacher)
            ROLE_STUDENT -> context.getString(R.string.role_student)
            //TODO report letters to firebase to analyze
            //ROLE_LEADING -> context.getString(R.string.role_leading)
            ROLE_SYSTEM -> context.getString(R.string.role_system)
            else -> context.getString(R.string.role_oder)
        }
    }

    /**@return string in format 1.A first name last name*/
    fun getClassAndRole(context: Context): String {
        return "${classInfo.shortcut} - ${translateUserType(context)}"
    }

    fun decodedCampaign(): JSONObject? {
        return try {
            JSONObject(String(Base64.decode(campaignCategory, Base64.DEFAULT)))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //modules and features
    @IgnoredOnParcel
    private val modules = modulesFeatures.map { it.moduleName }.toHashSet()

    /**if whole module is enabled, for example komens, timetable, ...*/
    fun isModuleEnabled(moduleName: String): Boolean {
        return modules.contains(moduleName)
    }

    /** runs given code if module is enabled
     * @return data returned from code given or null if nothing was executed*/
    inline fun <E> runIfModuleEnabled(moduleName: String, todo: (() -> E)): E? {
        if (isModuleEnabled(moduleName))
            return todo()
        return null
    }

    /** holds list of all features*/
    @IgnoredOnParcel
    private val allFeatures = modulesFeatures.map { it.featureName }
    fun getAllFeatures(): List<String> = allFeatures


    /**@return in exact feature is available, for example showMarks, showFinalMarks, ...*/
    fun isFeatureEnabled(featureType: String): Boolean {
        return getAllFeatures().contains(featureType)
    }

    /** runs given code if feature is enabled
     * @return data returned from code given or null if nothing was executed*/
    inline fun <E> runIfFeatureEnabled(moduleName: String, todo: (() -> E)): E? {
        if (isFeatureEnabled(moduleName))
            return todo()
        return null
    }
}
