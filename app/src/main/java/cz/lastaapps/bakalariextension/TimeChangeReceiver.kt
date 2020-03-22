package cz.lastaapps.bakalariextension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import cz.lastaapps.bakalariextension.api.timetable.TTNotifiService
import cz.lastaapps.bakalariextension.login.LoginData

class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = TimeChangeReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Time changed, updating timetable notification")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(Intent(context, TTNotifiService::class.java))
        else
            context.startService(Intent(context, TTNotifiService::class.java))

        LoginData.tokenExpiration = 0L
    }
}