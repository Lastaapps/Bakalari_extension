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

package cz.lastaapps.bakalari.app.services.timetablenotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.tools.normalizeID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TTReceiver : BroadcastReceiver() {

    companion object {
        private val TAG get() = TTReceiver::class.java.simpleName

        /**Used for updating during day*/
        val REQUEST_CODE = R.id.request_code_tt_receiver_normal.normalizeID()

        /**Used to pending intent started every day*/
        val REQUEST_CODE_DAILY = R.id.request_code_tt_receiver_daily.normalizeID()
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Timetable's intent received")

        //locks wakelock so service can start
        val pm =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl =
            pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "cz.lastaapps.bakalari.app.services.timetablenotification:TTReceiver"
            )

        //it can take sometimes long
        val timeout = 5000L

        //starts service with timetable notification
        if (TTNotifyService.startService(context)) {
            wl.acquire(timeout)

            //waits until service goes foreground
            CoroutineScope(Dispatchers.Default).launch {

                for (i in 0 until timeout) {
                    delay(1)

                    try {
                        if (wl != null && wl.isHeld) {
                            if (TTNotifyService.isServiceRunningInForeground)
                                wl.release()
                        } else
                            break
                    } catch (e: Exception) {
                        break
                    }
                }
                Log.i(TAG, "Wakelock released")
            }
        }

    }
}
