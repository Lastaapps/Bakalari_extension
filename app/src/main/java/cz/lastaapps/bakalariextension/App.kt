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

package cz.lastaapps.bakalariextension

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import cz.lastaapps.bakalariextension.tools.LocaleManager
import cz.lastaapps.bakalariextension.tools.MySettings

/**
 * Stores static context
 */
class App : Application() {

    companion object {
        private val TAG = App::class.java.simpleName

        //used during backup
        var tempContext: Context? = null

        //static reference to app
        lateinit var app: App

        //static reference to app context
        val context: Context
            get() {
                if (tempContext != null)
                    return tempContext!!
                return app.applicationContext
            }

        //reference to Firebase analytics
        val firebaseAnalytics: FirebaseAnalytics
            get() = app.firebaseAnalytics

        //static reference to app resources
        val resources: Resources
            get() = context.resources

        /**@return string from app's resources*/
        fun getString(id: Int): String {
            return resources.getString(id)
        }

        /**@return string array from app's resources*/
        fun getStringArray(id: Int): Array<String> {
            return resources.getStringArray(id)
        }

        /**@return color from app's resources*/
        fun getColor(id: Int): Int {
            return ContextCompat.getColor(context, id)
        }

        /**@return drawable from app's resources*/
        fun getDrawable(id: Int): Drawable? {
            return ContextCompat.getDrawable(context, id)
        }

        /**@return dimen from app's resources*/
        fun getDimension(id: Int): Int {
            return resources.getDimensionPixelSize(id)
        }
    }

    private val firebaseAnalytics: FirebaseAnalytics
        //firebase init
        get() = FirebaseAnalytics.getInstance(this)

    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App object created")

        //sets selected theme from Settings
        MySettings(this).apply {
            initSettings()
            updateDarkTheme()
        }

    }

    override fun attachBaseContext(base: Context) {
        //updates Application's context language
        super.attachBaseContext(
            {
                val context = LocaleManager.updateLocale(base)

                context
            }.invoke()
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //keeps language updated
        LocaleManager.updateLocale(
            this
        )
    }
}