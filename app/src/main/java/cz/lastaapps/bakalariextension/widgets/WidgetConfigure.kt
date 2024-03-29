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


package cz.lastaapps.bakalariextension.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.WidgetConfigureActivityBinding
import cz.lastaapps.bakalariextension.tools.MySettings


/**
 * The parent configuration screen for all widgets.
 */
open class WidgetConfigure(private val config: WidgetConfigurePreferences) : AppCompatActivity(),
    View.OnClickListener {

    companion object {
        private val TAG = WidgetConfigure::class.java.simpleName
    }

    //id of the widget from intent
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    //view structure
    private lateinit var binding: WidgetConfigureActivityBinding

    //used to place widget into place
    private lateinit var mAppWidgetManager: AppWidgetManager
    private lateinit var mAppWidgetHost: AppWidgetHost
    private lateinit var hostView: AppWidgetHostView

    //the views for the widget
    private lateinit var remoteViews: RemoteViews

    //updates data in SharedPreferences
    private val updater = Updater(config)

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        Log.i(TAG, "Creating activity")

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        //updates activity theme
        AppCompatDelegate.setDefaultNightMode(
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            else
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        //inflates view layout
        binding = DataBindingUtil.setContentView(this, R.layout.widget_configure_activity)

        //action setup
        binding.apply {
            //closes and sends respond to launcher
            addButton.setOnClickListener(this@WidgetConfigure)

            //updates widget preview when seekBars move
            val listener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    updateWidget()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }

            alphaBar.setOnSeekBarChangeListener(listener)
            themeBar.setOnSeekBarChangeListener(listener)
        }

        // Find the widget id from the intent.
        intent.extras?.let {
            appWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        createWidget(appWidgetId)
        updateWidget()
    }

    override fun onDestroy() {
        super.onDestroy()

        //turn on app theme back
        if (isFinishing)
            MySettings(this).updateDarkTheme()
    }

    /**creates appWidgetHost - view with widget*/
    private fun createWidget(appWidgetId: Int) {

        mAppWidgetManager = AppWidgetManager.getInstance(this)
        mAppWidgetHost = AppWidgetHost(this, R.id.APPWIDGET_HOST_ID)

        val appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId)

        //creates view with widget inside
        hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo)
        hostView.setAppWidget(appWidgetId, appWidgetInfo)
        binding.widgetFrame.addView(hostView)

        //later updated and placed inside
        remoteViews = RemoteViews(packageName, config.getLayoutId())
        hostView.updateAppWidget(remoteViews)

        //listening for updates (like service adapters)
        mAppWidgetHost.startListening()
    }

    /**updates widget - mostly theme*/
    private fun updateWidget() {

        Log.i(TAG, "The widget configuration done")

        //saves seekBars positions
        updater.setPref(
            updater.ALPHA,
            appWidgetId,
            binding.alphaBar.progress
        )

        updater.setPref(
            updater.THEME,
            appWidgetId,
            binding.themeBar.progress
        )

        //updates remote views
        config.updateRemoteViews(remoteViews, appWidgetId, this)

        //apply remote views to widgetHost
        hostView.updateAppWidget(null)
        hostView.updateAppWidget(remoteViews)
    }

    /**Closes and places widget into launcher*/
    override fun onClick(v: View?) {
        //saves seekBars values
        updater.setPref(
            updater.ALPHA,
            appWidgetId,
            binding.alphaBar.progress
        )
        updater.setPref(
            updater.THEME,
            appWidgetId,
            binding.themeBar.progress
        )

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        config.updateAppWidget(
            this,
            appWidgetManager,
            appWidgetId
        )

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    /**Contains config specific for widget given*/
    interface WidgetConfigurePreferences {
        /**the key for SharedPreferences for widget type*/
        fun getSPKey(): String
        /**just for sure, the beginning of each entry in SP*/
        fun getPrefix(): String
        /**Remote views are inflated from that*/
        fun getLayoutId(): Int
        /**update is required, theme or alpha changed*/
        fun updateRemoteViews(remoteViews: RemoteViews, widgetId: Int, context: Context)
        /**widget should be placed, updates it through appWidgetManager*/
        fun updateAppWidget(context: Context, manager: AppWidgetManager, widgetId: Int)
    }

    /**holds methods for setting the theme of a widget*/
    open class Updater(private val config: WidgetConfigurePreferences) {

        //Shared Preferences keys
        val THEME = "theme"
        val ALPHA = "alpha"

        //Shared Preferences values for theme
        val LIGHT = 0
        val SYSTEM = 1
        val DARK = 2

        /**Sets value for widget given*/
        open fun setPref(key: String, appWidgetId: Int, state: Int) {
            val prefs =
                App.context.getSharedPreferences(config.getSPKey(), Context.MODE_PRIVATE).edit()
            prefs.putInt(config.getPrefix() + appWidgetId + key, state)
            prefs.apply()
        }

        /**Gets value for widget given*/
        open fun getPref(key: String, appWidgetId: Int): Int {
            val prefs = App.context.getSharedPreferences(config.getSPKey(), Context.MODE_PRIVATE)
            return prefs.getInt(
                config.getPrefix() + appWidgetId + key,
                SYSTEM
            )
        }

        /**Applies alpha on a color from resources*/
        open fun applyAlpha(widgetId: Int, colorId: Int): Int {
            val color = App.getColor(colorId)
            return Color.argb(
                getPref(
                    ALPHA,
                    widgetId
                ),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            //return App.getColor(colorId)// and (getPref(ALPHA, widgetId) shl 24)
        }

        /**@return if widget should use light theme*/
        open fun isLight(widgetId: Int): Boolean {
            return when (getPref(
                THEME,
                widgetId
            )) {
                LIGHT -> true
                DARK -> false
                SYSTEM -> {
                    val currentMode: Int =
                        App.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                    Configuration.UI_MODE_NIGHT_NO == currentMode
                }
                else -> true
            }
        }
    }
}