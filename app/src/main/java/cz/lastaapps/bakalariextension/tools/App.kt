package cz.lastaapps.bakalariextension.tools

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import com.jakewharton.threetenabp.AndroidThreeTen

/**
 * Stores static context
 */
class App : Application() {

    companion object {
        private val TAG = App::class.java.simpleName

        lateinit var app: App

        val context: Context
        get() {
            return app.applicationContext
        }

        val resources: Resources
        get() {
            return app.resources
        }

        fun getString(id: Int): String {
            return resources.getString(id)
        }

        fun getStringArray(id: Int): Array<String> {
            return resources.getStringArray(id)
        }

        fun getColor(id: Int): Int {
            return resources.getColor(id)
        }

        fun getDrawable(id: Int): Drawable {
            return resources.getDrawable(id)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App object created")
        app = this
        AndroidThreeTen.init(this);
    }
}