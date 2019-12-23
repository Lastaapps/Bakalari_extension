package cz.lastaapps.bakalariextension

import android.app.Application
import android.content.Context

class App : Application() {

    companion object {
        lateinit var app: App
        fun appContext(): Context {
            return app.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this
    }
}