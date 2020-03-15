package cz.lastaapps.bakalariextension.api.timetable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class TTReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = TTReceiver::class.java.simpleName
        const val  REQUEST_CODE = 89661
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Timetable's intent received")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(Intent(context, TTNotifiService::class.java))
        else
            context.startService(Intent(context, TTNotifiService::class.java))
    }
}
