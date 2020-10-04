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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.widgets.smalltimetable.SmallTimetableWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**Manages update of all widgets*/
class WidgetUpdater : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.i(TAG, "Updating widgets")

        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        SmallTimetableWidget.update(context)
        //locks wakelock to give enough time to finish widget update
        val wait = goAsync()

        val timeout = 1000L

        //makes sure wakelock is released
        CoroutineScope(Dispatchers.Default).launch {

            delay(timeout)
            wait.finish()
        }
    }

    companion object {

        private val TAG = WidgetUpdater::class.java.simpleName
        private const val REQUEST_CODE = 3

        /**call #update and #setup methods*/
        fun updateAndSetup(context: Context) {
            update(context)
            setup(context)
        }

        /**Updates widgets*/
        fun update(context: Context) {

            Log.i(TAG, "Requesting widget update")
            val intent = Intent(context, WidgetUpdater::class.java)

            context.sendBroadcast(intent)
        }

        /**Sets up alarm updating widget at midnight*/
        private fun setup(context: Context) {
            Log.i(TAG, "Setting up intent to update widgets later")

            //starts this Broadcast receiver
            val intent = Intent(context, WidgetUpdater::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            //midnight in Czech republic
            val time = ZonedDateTime.of(
                LocalDate.now().plusDays(1),
                LocalTime.MIDNIGHT,
                TimeTools.CET
            )

            //sets alarm
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            when {
                Build.VERSION.SDK_INT >= 23 -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        time.toInstant().toEpochMilli(),
                        pendingIntent
                    )
                }
                Build.VERSION.SDK_INT >= 19 -> {
                    alarmManager.setExact(
                        AlarmManager.RTC,
                        time.toInstant().toEpochMilli(),
                        pendingIntent
                    )
                }
                else -> {
                    alarmManager.set(
                        AlarmManager.RTC,
                        time.toInstant().toEpochMilli(),
                        pendingIntent
                    )
                }
            }
        }
    }
}
