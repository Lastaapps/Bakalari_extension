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
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.util.Log
import java.util.*

/**Updates context with new language based on Settings*/
class LocaleManager {

    companion object {

        private val TAG = LocaleManager::class.java.simpleName

        /**
         * update language
         */
        fun updateLocale(
            context: Context
        ): Context {

            val locale = Locale(getLanguagePref(context))
            Locale.setDefault(locale)
            val res: Resources = context.resources
            val config = Configuration(res.configuration)

            Log.i(TAG, "Changing language to ${locale.language}")

            return when {
                Build.VERSION.SDK_INT >= 24 -> {
                    config.setLocale(locale)
                    val localeList = LocaleList(locale)
                    LocaleList.setDefault(localeList)
                    config.setLocales(localeList)
                    context.createConfigurationContext(config)
                }
                Build.VERSION.SDK_INT >= 17 -> {
                    config.setLocale(locale)
                    context.createConfigurationContext(config)
                }
                else -> {
                    config.locale = locale
                    res.updateConfiguration(config, res.displayMetrics)
                    context
                }
            }
        }

        /**@return language got from Settings*/
        private fun getLanguagePref(context: Context): String {
            return MySettings(context).getLanguageCode()
        }
    }
}