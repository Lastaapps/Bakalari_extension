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


package cz.lastaapps.bakalariextension.ui.timetable.small.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.BaseActivity

/**
 * The configuration screen for the Small timetable AppWidget.
 */
class SmallTimetableWidgetConfigure : BaseActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    private lateinit var backgroundAlphaBar: SeekBar
    private lateinit var themeBar: SeekBar
    
    private var onClickListener = View.OnClickListener {
        val context = this@SmallTimetableWidgetConfigure

        setPref(ALPHA, appWidgetId, backgroundAlphaBar.progress)
        setPref(THEME, appWidgetId, themeBar.progress)

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(
            context,
            appWidgetManager,
            appWidgetId
        )

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        setContentView(R.layout.widget_small_timetable_configure)

        backgroundAlphaBar = findViewById(R.id.alpha_bar)
        themeBar = findViewById(R.id.theme_bar)

        findViewById<View>(R.id.add_button).setOnClickListener(onClickListener)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

}

private const val PREFS_NAME = "timetable_small_widget"
private const val PREF_PREFIX_KEY = "appwidget_"
private const val THEME = "theme"
private const val ALPHA = "alpha"

private const val LIGHT = 0
private const val SYSTEM = 1
private const val DARK = 2

internal fun setPref(key: String, appWidgetId: Int, state: Int) {
    val prefs = App.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.putInt(PREF_PREFIX_KEY + appWidgetId + key, state)
    prefs.apply()
}

internal fun getPref(key: String, appWidgetId: Int): Int {
    val prefs = App.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + key, SYSTEM)
}

internal fun applyAlpha(widgetId: Int, colorId: Int): Int {
    val color = App.getColor(colorId)
    return Color.argb(getPref(ALPHA, widgetId),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
    //return App.getColor(colorId)// and (getPref(ALPHA, widgetId) shl 24)
}

internal fun isLight(widgetId: Int): Boolean {
    return when (getPref(THEME, widgetId)) {
        LIGHT -> true
        DARK -> false
        SYSTEM -> {
            val currentMode: Int = App.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            Configuration.UI_MODE_NIGHT_NO == currentMode
        }
        else -> true
    }
}
