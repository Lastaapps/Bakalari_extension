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

package cz.lastaapps.bakalariextension

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.timetable.small.widget.SmallTimetableWidget
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZonedDateTime

/**Manages update of all widgets*/
class WidgetUpdater : BroadcastReceiver() {

    //TODO add wakelock - first need to check if working with notifications
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        SmallTimetableWidget.update(context)
    }

    companion object {

        private const val REQUEST_CODE = 3

        /**call #update and #setup methods*/
        fun updateAndSetup(context: Context) {
            update(context)
            setup(context)
        }

        /**Updates widgets*/
        fun update(context: Context) {
            val intent = Intent(context, WidgetUpdater::class.java)

            context.sendBroadcast(intent)
        }

        /**Sets up alarm updating widget at midnight*/
        fun setup(context: Context) {
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
