package cz.lastaapps.bakalariextension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.lastaapps.bakalariextension.api.timetable.TTNotifiService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = BootReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed intent received")

            TTNotifiService.startService(context)
        }
    }
}
