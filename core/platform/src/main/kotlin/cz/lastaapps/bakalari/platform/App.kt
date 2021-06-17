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

package cz.lastaapps.bakalari.platform

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import cz.lastaapps.bakalari.settings.LocaleManager
import cz.lastaapps.bakalari.settings.MySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Stores static context
 */
class App : Application() {

    companion object {
        private val TAG get() = App::class.simpleName

        //used during backup
        @SuppressLint("StaticFieldLeak")
        var tempContext: Context? = null

        //static reference to app
        @SuppressLint("StaticFieldLeak")
        lateinit var app: App

        //static reference to app context
        val context: Context
            get() {
                if (tempContext != null)
                    return tempContext!!
                return app.applicationContext
            }

        //static reference to app resources
        val resources: Resources
            get() = context.resources

        /**@return string from app's resources*/
        fun getString(@StringRes id: Int): String {
            return resources.getString(id)
        }

        /**@return string array from app's resources*/
        fun getStringArray(@ArrayRes id: Int): Array<String> {
            return resources.getStringArray(id)
        }

        /**@return color from app's resources*/
        fun getColor(@ColorRes id: Int): Int {
            return ContextCompat.getColor(context, id)
        }

        /**@return drawable from app's resources*/
        fun getDrawable(@DrawableRes id: Int): Drawable? {
            return ContextCompat.getDrawable(context, id)
        }

        /**@return dimen from app's resources*/
        fun getDimension(@DimenRes id: Int): Int {
            return resources.getDimensionPixelSize(id)
        }

        val coroutineScope = CoroutineScope(Dispatchers.Default)

        val onCreateTasks: MutableList<(Application, CoroutineScope) -> Unit> = mutableListOf()
        val afterCreateTasks: MutableList<suspend (Application, CoroutineScope) -> Unit> =
            mutableListOf()

        @SuppressLint("StaticFieldLeak")
        @Suppress("DEPRECATION")
        private var appSettings: MySettings? = null
        internal fun getAppSettings(context: Context): MySettings = synchronized(this) {
            if (appSettings == null)
                appSettings = MySettings.createSettings(context)

            appSettings!!
        }
    }

    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App object created")

        coroutineScope.launch(Dispatchers.Default) {
            val config = BundledEmojiCompatConfig(this@App)
                .setReplaceAll(true)
            EmojiCompat.init(config)
        }

        onCreateTasks.forEach {
            it(this, coroutineScope)
        }

        afterCreateTasks.forEach {
            coroutineScope.launch(Dispatchers.Main) {
                it(this@App, this)
            }
        }
    }

    override fun attachBaseContext(base: Context) {

        //updates Application's context language
        val localizedContext = LocaleManager.updateLocale(base, getAppSettings(base))
        super.attachBaseContext(localizedContext)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //keeps language updated
        LocaleManager.updateLocale(this, MySettings.withAppContext())
    }
}

fun MySettings.Companion.withAppContext() = App.getAppSettings(App.context)

