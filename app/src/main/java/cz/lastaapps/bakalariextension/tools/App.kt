package cz.lastaapps.bakalariextension.tools

import android.app.Application
import android.content.Context
import android.util.Log

/**
 * Stores static context
 */
class App : Application() {

    companion object {
        private val TAG = "${App::class.java.simpleName}"

        lateinit var app: App
        fun appContext(): Context {
            return app.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App object created")
        app = this
    }
}