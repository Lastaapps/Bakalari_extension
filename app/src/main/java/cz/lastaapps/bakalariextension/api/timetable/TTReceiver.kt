package cz.lastaapps.bakalariextension.api.timetable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TTReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = TTReceiver::class.java.simpleName
        /**Used to pending intent started every day*/
        const val  REQUEST_CODE_DAILY = 89660
        /**Used for updating during day*/
        const val  REQUEST_CODE = 89661
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Timetable's intent received")

        TTNotifiService.startService(context)
    }
}
