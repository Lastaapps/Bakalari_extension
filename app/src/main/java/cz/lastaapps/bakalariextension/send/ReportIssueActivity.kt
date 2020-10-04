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

package cz.lastaapps.bakalariextension.send

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import cz.lastaapps.bakalariextension.BuildConfig
import cz.lastaapps.bakalariextension.CurrentUser
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.ActivityReportBinding
import cz.lastaapps.bakalariextension.tools.BaseActivity
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.login.LoginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sends error report to Firebase database
 * Limited to 1 per day
 */
class ReportIssueActivity : BaseActivity() {

    companion object {
        private val TAG = ReportIssueActivity::class.java.simpleName

        private const val SP_KEY = "REPORT_ISSUE"
        private const val SP_DATE_KEY = "LAST_SENT"
    }

    private lateinit var binding: ActivityReportBinding

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating activity")

        //checks if there was send message today
        if (timeCheck()) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_report)

            database = FirebaseDatabase.getInstance().reference

            //sends data to Firebase
            binding.reportFab.setOnClickListener {

                lifecycleScope.launch(Dispatchers.Default) {
                    send()

                    getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(
                            SP_DATE_KEY,
                            ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        )
                        .apply()
                }
            }

            //Opens new issue on Github
            binding.githubFab.setOnClickListener {
                val url = "https://github.com/Lastaapps/Bakalari_extension/issues/new"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } else {

            Log.i(TAG, "Time check failed")

            //If limit per day was reached
            AlertDialog.Builder(this)
                .setMessage(R.string.report_overload)
                .setPositiveButton(R.string.report_go_back) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .create()
                .show()
        }

    }

    /**
     * @return If message was sent today, or if user is moving through time in settings
     */
    private fun timeCheck(): Boolean {

        val lastSent = getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
            .getLong(SP_DATE_KEY, 0)

        val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastSent), TimeTools.UTC)
        val now = TimeTools.now

        if (date.isAfter(now))
            return false

        return date.toLocalDate() != now.toLocalDate()
    }

    /**
     * Sends needed data to Firebase
     */
    private suspend fun send() {
        Log.i(TAG, "Sending data")

        val email = findViewById<EditText>(R.id.email).text.trim().toString()
        val message = findViewById<EditText>(R.id.message).text.trim().toString()

        if (message != "") {
            try {
                val id = TimeTools.format(TimeTools.now, TimeTools.COMPLETE_FORMAT) + "_" +
                        database.push().key.toString()

                //if user allowed to send town and school for analytics
                val canSendSchool = {
                    val sett = MySettings.withAppContext()
                    sett.getSP().getBoolean(sett.SEND_TOWN_SCHOOL, false)
                }.invoke()

                //data to be sent
                val data = Message(
                    date = TimeTools.format(TimeTools.now, TimeTools.COMPLETE_FORMAT),
                    messageId = id,
                    email = email,
                    message = message,
                    phoneId = {
                        (LoginData.userID + LoginData.town + LoginData.school).hashCode().toString()
                    }.invoke(),
                    phoneType = getDeviceName(),
                    androidVersion = Build.VERSION.SDK_INT,
                    appVersionCode = BuildConfig.VERSION_CODE,
                    appVersionName = BuildConfig.VERSION_NAME,
                    school = if (canSendSchool) LoginData.school else "disabled",
                    town = if (canSendSchool) LoginData.town else "disabled",
                    url = LoginData.url,
                    bakalariVersion = LoginData.apiVersion
                )

                addJSONs(data)

                //sends data
                database.child("report").child(id).setValue(data)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        R.string.idea_thanks,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        R.string.report_no_internet,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ReportIssueActivity,
                    R.string.idea_empty,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun addJSONs(data: Message) {

        CurrentUser.database?.jsonStorageRepository?.let { repo ->

            if (binding.includeTimetable.isChecked)
                data.timetables = repo.getAllTimetables().map { it.toString().removeNames() }

            if (binding.includeHomework.isChecked)
                data.user = String.format("%s", repo.getUser()) //null safety
                    .removeNames()
                    .removeJsonValue("FullName", "full name")
                    .removeJsonValue("SchoolOrganizationName", "school name")

            if (binding.includeHomework.isChecked)
                data.homeworkList =
                    String.format("%s", repo.getHomework()).removeNames() //null safety

            if (binding.includeMarks.isChecked)
                data.marks = String.format("%s", repo.getMarks()).removeNames() //null safety

            if (binding.includeSubject.isChecked)
                data.subjects = String.format("%s", repo.getSubjects()).removeNames() //null safety

        }
    }

    private fun String.removeNames() = removeJsonValue("Name")

    private fun String.removeJsonValue(key: String, toReplace: String = "Jára Cimrman"): String {
        val toSearch = "\"$key\":\""
        val temp = "G|M?c1y-"
        var index = 0
        var edited = this

        while (true) {
            index = edited.indexOf(toSearch)
            if (index < 0) break

            val valueIndex = index + toSearch.length
            val lastIndex = edited.indexOf('"', valueIndex)

            edited = edited.substring(0, index) + temp + toReplace + edited.substring(
                lastIndex,
                edited.length
            )
        }

        edited = edited.replace(temp, toSearch)

        return edited
    }

    /**Data structure of the data to be send*/
    @IgnoreExtraProperties
    data class Message constructor(
        var date: String? = "",
        var messageId: String? = "",
        var email: String? = "",
        var message: String? = "",
        var phoneId: String? = "",
        var phoneType: String? = "",
        var androidVersion: Int? = -1,
        var appVersionCode: Int? = -1,
        var appVersionName: String? = "",
        var school: String? = "",
        var town: String? = "",
        var url: String? = "",
        var bakalariVersion: String? = "",
        var user: String? = null,
        var timetables: List<String>? = null,
        var marks: String? = null,
        var homeworkList: String? = null,
        var subjects: String? = null
    )

    /**@return device name*/
    private fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else capitalize(manufacturer) + " " + model
    }

    /**@return Who knows...*/
    private fun capitalize(str: String): String {
        if (TextUtils.isEmpty(str)) {
            return str
        }
        val arr = str.toCharArray()
        var capitalizeNext = true
        val phrase = StringBuilder()
        for (c in arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c))
                capitalizeNext = false
                continue
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true
            }
            phrase.append(c)
        }
        return phrase.toString()
    }
}
