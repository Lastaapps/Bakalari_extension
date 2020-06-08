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

package cz.lastaapps.bakalariextension.tools

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.codekidlabs.storagechooser.Content
import com.codekidlabs.storagechooser.StorageChooser
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

/**Contains app settings and methods to access them usefully*/
class MySettings(val context: Context) {

    companion object {
        private val TAG = MySettings::class.java.simpleName
        const val FILE_SELECT_CODE = 65132

        private var withAppContext: MySettings? = null
        fun withAppContext(): MySettings {
            if (withAppContext == null) {
                withAppContext = MySettings(App.context)
            }
            return withAppContext!!
        }
    }

    //preferences keys
    val NOTIFICATION_SETTINGS
        get() = getString(R.string.sett_key_notification_settings)
    val MOBILE_DATA
        get() = getString(R.string.sett_key_mobile_data)
    val DARK_MODE
        get() = getString(R.string.sett_key_dark_mode)
    val LANGUAGE
        get() = getString(R.string.sett_key_language)
    val LOGOUT
        get() = getString(R.string.sett_key_log_out)
    val DOWNLOAD_LOCATION
        get() = getString(R.string.sett_key_download_location)
    val TIMETABLE_DAY
        get() = getString(R.string.sett_key_timetable_day)
    val TIMETABLE_NOTIFICATION
        get() = getString(R.string.sett_key_timetable_notification)
    val TIMETABLE_PREVIEW
        get() = getString(R.string.sett_key_timetable_preview)
    val MARKS_SHOW_NEW
        get() = getString(R.string.sett_key_marks_new_mark)
    val SEND_TOWN_SCHOOL
        get() = getString(R.string.sett_key_send_town_school)
    val RESET
        get() = getString(R.string.sett_key_reset)
    val SHOW_WHATS_NEW
        get() = getString(R.string.sett_key_show_whats_new)
    val ABOUT
        get() = getString(R.string.sett_key_about)
    val LICENSE
        get() = getString(R.string.sett_key_license)

    /**Inits settings if they aren't yet*/
    fun initSettings() {
        val sp = getSP()
        if (sp.all.isEmpty()) {
            Log.i(TAG, "Initializing settings to default values")

            val editor = sp.edit()

            /*
            Log.d(TAG, sp.getBoolean(MOBILE_DATA, false).toString())
            Log.d(TAG, sp.getString(LANGUAGE, ""))
            Log.d(TAG, sp.getString(DARK_MODE, ""))
            Log.d(TAG, sp.getString(TIMETABLE_DAY, ""))
            Log.d(TAG, sp.getBoolean(TIMETABLE_NOTIFICATION, false).toString())
            Log.d(TAG, sp.getBoolean(SEND_TOWN_SCHOOL, false).toString())
            */

            //sets default values
            if (sp.getString(DARK_MODE, "") == "")
                editor.putString(DARK_MODE, getArray(R.array.sett_dark_mode)[2])

            if (sp.getString(LANGUAGE, "") == "")
                editor.putString(LANGUAGE, getArray(R.array.sett_language)[0])

            if (sp.getString(DOWNLOAD_LOCATION, "") == "")
                editor.putString(
                    DOWNLOAD_LOCATION,
                    if (Build.VERSION.SDK_INT >= 29) {
                        ""
                    } else {
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
                    }
                )

            if (sp.getString(TIMETABLE_DAY, "") == "")
                editor.putString(TIMETABLE_DAY, getArray(R.array.sett_timetable_day)[2])

            if (sp.getString(TIMETABLE_PREVIEW, "") == "")
                editor.putString(TIMETABLE_PREVIEW, getArray(R.array.sett_timetable_preview)[0])

            if (sp.getString(MARKS_SHOW_NEW, "") == "")
                editor.putString(MARKS_SHOW_NEW, getString(R.string.sett_marks_new_mark_default))

            editor.apply()
        }
    }

    /**@return currently selected language code (en, cs, ...)*/
    fun getLanguageCode(value: String = getSP().getString(LANGUAGE, "") ?: ""): String {
        val languageNamesArray: Array<String> = getArray(R.array.sett_language)
        val index = languageNamesArray.indexOf(value)

        val languageCodesArray: Array<String> = getArray(R.array.sett_language_codes)

        //sets system default option based on actual system settings
        val def =
            //Locale.getDefault().language
            if (Build.VERSION.SDK_INT >= 24)
                Resources.getSystem().configuration.locales.get(0).language
            else
                Resources.getSystem().configuration.locale.language

        //updates system default or lets en be as default, if system language is not supported
        if (languageCodesArray.contains(def)) {
            languageCodesArray[0] = def
        } //else 'en' is set as default

        return languageCodesArray[index.coerceAtLeast(0)]
    }

    /**Updates app's theme based on settings*/
    fun updateDarkTheme(value: String = getSP().getString(DARK_MODE, "").toString()) {
        val array: Array<String> = getArray(R.array.sett_dark_mode)

        val toChange = when (array.indexOf(value)) {
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            else ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
        Log.i(TAG, "Dark mode set to $toChange")

        AppCompatDelegate.setDefaultNightMode(toChange)

    }

    /**@return when should be timetable for the next week shown*/
    fun getTimetableDayOffset(): Int {
        val array = getArray(R.array.sett_timetable_day)
        return 2 - array.indexOf(getSP().getString(TIMETABLE_DAY, "")).also {
            Log.i(TAG, "Timetable offset set to $it")
        }
    }

    /**@return if timetable notification should be shown*/
    fun isTimetableNotificationEnabled(): Boolean {
        return getSP().getBoolean(TIMETABLE_NOTIFICATION, true)
    }

    /**@return if the timetable preview for tomorrow should be shown*/
    fun showTomorrowsPreview(now: ZonedDateTime, lessonsEnd: ZonedDateTime): ZonedDateTime {

        if (lessonsEnd.dayOfWeek == DayOfWeek.FRIDAY) return now
        if (lessonsEnd.dayOfWeek == DayOfWeek.SATURDAY) return now.plusDays(2)
        if (lessonsEnd.dayOfWeek == DayOfWeek.SUNDAY) return now.plusDays(1)

        if (now < lessonsEnd) return now

        val invalidDuration = when (getArray(R.array.sett_timetable_preview).indexOf(
            getSP().getString(TIMETABLE_PREVIEW, "")
        )) {
            0 -> Duration.between(lessonsEnd, TimeTools.toMidnight(lessonsEnd.plusDays(1)))
            1 -> Duration.ZERO
            2 -> Duration.ofHours(1)
            3 -> Duration.ofHours(2)
            4 -> Duration.ofHours(3)
            5 -> Duration.between(lessonsEnd, TimeTools.toMidnight(lessonsEnd).withHour(17))
            6 -> Duration.between(lessonsEnd, TimeTools.toMidnight(lessonsEnd).withHour(18))
            7 -> Duration.between(lessonsEnd, TimeTools.toMidnight(lessonsEnd).withHour(19))
            else -> return now
        }
        val currentDuration = Duration.between(lessonsEnd, now)

        return if (currentDuration >= invalidDuration) {
            now.plusDays(1)
        } else {
            now
        }
    }

    /**@return for how many days should be mark shown as new*/
    fun getNewMarkDuration(): Int {
        return getSP().getString(MARKS_SHOW_NEW, "2")!!.toInt()
    }

    fun getDownloadLocation(): String =
        getSP().getString(DOWNLOAD_LOCATION, "")!!

    fun setDownloadLocation(path: String) {
        getSP().edit().putString(DOWNLOAD_LOCATION, path).apply()
    }

    /**Pops up dialog, in which user chooses default download location*/
    fun chooseDownloadDirectory(activity: Activity, onDone: ((newPath: String) -> Unit)) {

        Toast.makeText(activity, R.string.select_download_location, Toast.LENGTH_LONG).show()

        if (Build.VERSION.SDK_INT >= 29) {

            //    val intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            //    intent.putExtra("CONTENT_TYPE", DocumentsContract.Document.MIME_TYPE_DIR)
            //    intent.addCategory(Intent.CATEGORY_DEFAULT)
            //    startActivityForResult(intent, REQUEST_CODE)

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            //intent.type = DocumentsContract.Document.MIME_TYPE_DIR
            //intent.addCategory(Intent.CATEGORY_OPENABLE)

            try {
                //response
                activity.startActivityForResult(
                    //Intent.createChooser(intent, "Select a File to Upload"),
                    intent,
                    FILE_SELECT_CODE
                )
            } catch (ex: ActivityNotFoundException) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(
                    activity,
                    "Please install a File Manager.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val chooser = StorageChooser.Builder()
                .withActivity(activity)
                .withFragmentManager(activity.fragmentManager)
                .withMemoryBar(true)
                .allowCustomPath(true)
                .disableMultiSelect()
                .withContent(object : Content() {
                    init {
                        activity.apply {
                            selectLabel = getString(R.string.filechoooser_selectLabel)
                            createLabel = getString(R.string.filechoooser_createLabel)
                            newFolderLabel = getString(R.string.filechoooser_newFolderLabel)
                            cancelLabel = getString(R.string.filechoooser_cancelLabel)
                            overviewHeading = getString(R.string.filechoooser_overviewHeading)
                            internalStorageText =
                                getString(R.string.filechoooser_internalStorageText)
                            freeSpaceText = getString(R.string.filechoooser_freeSpaceText)
                            folderCreatedToastText =
                                getString(R.string.filechoooser_folderCreatedToastText)
                            folderErrorToastText =
                                getString(R.string.filechoooser_folderErrorToastText)
                            textfieldHintText = getString(R.string.filechoooser_textfieldHintText)
                            textfieldErrorText = getString(R.string.filechoooser_textfieldErrorText)
                        }
                    }
                })
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .allowAddFolder(true)
                .setDialogTitle(activity.getString(R.string.select_download_location))
                .withPredefinedPath(getDownloadLocation())
                .build()

            // Show dialog whenever you want by
            chooser.show()

            // get path that the user has chosen
            chooser.setOnSelectListener { path ->
                Log.i(TAG, "New download directory $path")
                setDownloadLocation(path)
                onDone(path)
            }
        }

    }

    /**@return Setting's shared preferences*/
    fun getSP(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**@return string from resources*/
    private fun getString(id: Int): String {
        return context.getString(id)
    }

    /**@return string array from resources*/
    private fun getArray(id: Int): Array<String> {
        return context.resources.getStringArray(id)
    }
}