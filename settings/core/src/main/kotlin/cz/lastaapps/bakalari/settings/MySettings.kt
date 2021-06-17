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

package cz.lastaapps.bakalari.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import cz.lastaapps.bakalari.tools.TimeTools.toMidnight
import cz.lastaapps.bakalari.tools.normalizeID
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

/**Contains app settings and methods to access them usefully*/
class MySettings private constructor(inputContext: Context) {

    private val context: Context =
        if (inputContext is Activity) inputContext.applicationContext
        else inputContext


    companion object {
        private val TAG get() = MySettings::class.java.simpleName
        val FILE_SELECT_CODE = R.id.request_code_settings_download_location.normalizeID()

        @Deprecated(
            "Use a singleton instead (located in *.platform.App.kt for example)",
            ReplaceWith(
                "MySettings.withAppContext()",
                "cz.lastaapps.bakalari.settings.MySettings\ncz.lastaapps.bakalari.platform"
            )
        )
        fun createSettings(context: Context): MySettings {
            return MySettings(context).also {
                it.initSettings()
            }
        }
    }

    //preferences keys
    //General
    val GENERAL by lazy { getString(R.string.sett_key_general_settings) }
    val NOTIFICATION_SETTINGS by lazy { getString(R.string.sett_key_notification_settings) }
    val MOBILE_DATA by lazy { getString(R.string.sett_key_mobile_data) }
    val DARK_MODE by lazy { getString(R.string.sett_key_dark_mode) }
    val LANGUAGE by lazy { getString(R.string.sett_key_language) }
    val DOWNLOAD_LOCATION by lazy { getString(R.string.sett_key_download_location) }

    val TIMETABLE_NOTIFICATION by lazy { getString(R.string.sett_key_timetable_notification) }

    val USER by lazy { getString(R.string.sett_key_user_settings) }
    val TIMETABLE_CATEGORY by lazy { getString(R.string.sett_key_timetable_category) }
    val TIMETABLE_DAY by lazy { getString(R.string.sett_key_timetable_day) }
    val TIMETABLE_PREVIEW by lazy { getString(R.string.sett_key_timetable_preview) }
    val MARKS_CATEGORY by lazy { getString(R.string.sett_key_marks_category) }
    val MARKS_SHOW_NEW by lazy { getString(R.string.sett_key_marks_new_mark) }
    val EVENTS_CATEGORY by lazy { getString(R.string.sett_key_events_category) }
    val EVENTS_SHOW_FOR_DAY by lazy { getString(R.string.sett_key_events_show_for_day) }

    val DATA_COLLECTION by lazy { getString(R.string.sett_key_data_collection_settings) }
    val SEND_TOWN_SCHOOL by lazy { getString(R.string.sett_key_send_town_school) }

    val RESET by lazy { getString(R.string.sett_key_reset) }
    val SHOW_WHATS_NEW by lazy { getString(R.string.sett_key_show_whats_new) }
    val ABOUT by lazy { getString(R.string.sett_key_about) }
    val LICENSE by lazy { getString(R.string.sett_key_license) }

    init {
        initSettings()
    }

    /**Inits settings if they aren't yet*/
    fun initSettings(force: Boolean = false) {
        val sp = getSP()
        if (sp.all.isEmpty() || force) {
            Log.i(TAG, "Initializing settings to default values")

            val editor = sp.edit()

            //sets default values
            if (sp.getString(DARK_MODE, "") == "")
                editor.putString(DARK_MODE, getArray(R.array.sett_dark_mode)[2])

            if (sp.getString(LANGUAGE, "") == "")
                editor.putString(LANGUAGE, getArray(R.array.sett_language)[0])

            if (sp.getString(TIMETABLE_DAY, "") == "")
                editor.putString(TIMETABLE_DAY, getArray(R.array.sett_timetable_day)[2])

            if (sp.getString(TIMETABLE_PREVIEW, "") == "")
                editor.putString(TIMETABLE_PREVIEW, getArray(R.array.sett_timetable_preview)[0])

            if (sp.getString(MARKS_SHOW_NEW, "") == "")
                editor.putString(MARKS_SHOW_NEW, getString(R.string.sett_marks_new_mark_default))

            if (sp.getString(EVENTS_SHOW_FOR_DAY, "") == "")
                editor.putString(EVENTS_SHOW_FOR_DAY, getArray(R.array.sett_events_show_for_day)[5])

            editor.apply()

            timetableNotificationAccountUUID
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

    var timetableNotificationAccountUUID: UUID?
        get() {
            val uuid = getSP().getString(TIMETABLE_NOTIFICATION, "") ?: ""
            if (uuid == "") return null
            return try {
                UUID.fromString(uuid)
            } catch (e: Exception) {
                Log.e(TAG, "Wrong data saved: $uuid")
                e.printStackTrace()
                null
            }
        }
        set(value) {
            val string = value?.toString() ?: ""
            getSP().edit().putString(TIMETABLE_NOTIFICATION, string).apply()

            //TODO reimplement using flows
            //updates service
            /*Handler(Looper.getMainLooper()).postDelayed( {
                TTNotifyService.startService(context)
            }, 250)//waits until data updates in SharedPreferences
            */
        }

    /**@return if the data for tomorrow should be shown
     * used with #TIMETABLE_PREVIEW and #EVENTS_SHOW_FOR_DAY */
    fun showTomorrow(
        now: ZonedDateTime,
        lessonsEnd: ZonedDateTime,
        settKey: String,
        arrayId: Int
    ): ZonedDateTime {

        if (lessonsEnd.dayOfWeek == DayOfWeek.SATURDAY) return now.plusDays(2)
        if (lessonsEnd.dayOfWeek == DayOfWeek.SUNDAY) return now.plusDays(1)

        if (now < lessonsEnd) return now

        val invalidDuration = when (getArray(arrayId).indexOf(
            getSP().getString(settKey, "")
        )) {
            0 -> Duration.between(lessonsEnd, lessonsEnd.plusDays(1).toMidnight())
            1 -> Duration.ZERO
            2 -> Duration.ofHours(1)
            3 -> Duration.ofHours(2)
            4 -> Duration.ofHours(3)
            5 -> Duration.between(now, lessonsEnd.toMidnight().withHour(17))
            6 -> Duration.between(now, lessonsEnd.toMidnight().withHour(18))
            7 -> Duration.between(now, lessonsEnd.toMidnight().withHour(19))
            else -> return now
        }
        val currentDuration = Duration.between(lessonsEnd, now)

        return if (currentDuration.seconds >= invalidDuration.seconds) {

            if (lessonsEnd.dayOfWeek != DayOfWeek.FRIDAY)
                now.plusDays(1)
            else
                now.plusDays(3)
        } else {
            now
        }
    }

    /** if week is required to get valid date for #showTomorrow()*/
    fun weekRequired(settKey: String, arrayId: Int): Boolean = (getArray(arrayId).indexOf(
        getSP().getString(settKey, "")
    ) in 1..4)


    /**@return for how many days should be mark shown as new*/
    fun getNewMarkDuration(): Int = getSP().getString(MARKS_SHOW_NEW, "2")!!.toInt()

    fun getDownloadLocation(): String = getSP().getString(DOWNLOAD_LOCATION, "")!!

    fun setDownloadLocation(path: String) =
        getSP().edit().putString(DOWNLOAD_LOCATION, path).apply()


    /**Pops up dialog, in which user chooses default download location*/
    @SuppressLint("InlinedApi")
    fun chooseDownloadDirectory(activity: FragmentActivity) {

        Toast.makeText(activity, R.string.select_download_location, Toast.LENGTH_LONG).show()

        val downloadReceiver = activity.registerForActivityResult(DownloadLocationContract()) { uri ->
            uri?.let {

                setDownloadLocation(uri.toString())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
        }

        try {
            //response
            downloadReceiver.launch("");

        } catch (ex: ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(
                activity,
                "Please install a File Manager.",
                Toast.LENGTH_SHORT
            ).show()

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