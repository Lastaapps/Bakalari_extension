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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import cz.lastaapps.bakalariextension.R

class Settings(val context: Context) {

    companion object {
        private val TAG = Settings::class.java.simpleName
    }

    //preferences keys
    val MOBILE_DATA
        get() = getString(R.string.sett_key_mobile_data)
    val DARK_MODE
        get() = getString(R.string.sett_key_dark_mode)
    val LANGUAGE
        get() = getString(R.string.sett_key_language)
    val LOGOUT
        get() = getString(R.string.sett_key_log_out)
    val TIMETABLE_DAY
        get() = getString(R.string.sett_key_timetable_day)
    val TIMETABLE_NOTIFICATION
        get() = getString(R.string.sett_key_timetable_notification)
    val SEND_TOWN_SCHOOL
        get() = getString(R.string.sett_key_send_town_school)

    /**Inits settings if they aren't yet*/
    fun initSettings() {
        val sp = getSP()
        if (sp.all.isEmpty()) {
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
            if (sp.getString(LANGUAGE, "") == "")
                editor.putString(LANGUAGE, getArray(R.array.sett_language)[0])

            if (sp.getString(DARK_MODE, "") == "")
                editor.putString(DARK_MODE, getArray(R.array.sett_dark_mode)[2])

            if (sp.getString(TIMETABLE_DAY, "") == "")
                editor.putString(TIMETABLE_DAY, getArray(R.array.sett_timetable_day)[2])

            editor.apply()
        }
    }

    /**@return currently selected language code (en, cs, ...)*/
    fun getLanguageCode(value: String = getSP().getString(LANGUAGE, "")?: ""): String {
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

        AppCompatDelegate.setDefaultNightMode(toChange)

    }

    /**@return when should be timetable for the next week shown*/
    fun getTimetableDayOffset(): Int {
        val array = getArray(R.array.sett_timetable_day)
        return 2 - array.indexOf(getSP().getString(TIMETABLE_DAY, ""))
    }

    /**if timetable notification should be shown*/
    fun isTimetableNotificationEnabled(): Boolean {
        return getSP().getBoolean(TIMETABLE_NOTIFICATION, true)
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