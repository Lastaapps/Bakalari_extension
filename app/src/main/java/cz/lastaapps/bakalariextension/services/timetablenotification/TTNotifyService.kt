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

package cz.lastaapps.bakalariextension.services.timetablenotification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import kotlin.math.floor

/**Shows notification generated in NotificationContent*/
class TTNotifyService : BaseService() {

    companion object {
        private val TAG = TTNotifyService::class.java.simpleName
        const val NOTIFICATION_CHANEL_ID = "timetable_notification_service"
        private const val NOTIFICATION_ID = 1
        /*private const val PENDING_INTENT_TRIGGER_NAME =
            "cz.lastaapps.action.TIMETABLE_NOTIFICATION_TRIGGER"*/

        /**used for starting service statically, can also stop service if it is disabled
         * @return if service was started or canceled*/
        fun startService(context: Context = App.context): Boolean {

            val intent = Intent(context, TTNotifyService::class.java)

            return if (LoginData.isLoggedIn()
                && Settings(context).isTimetableNotificationEnabled()) {

                //starts service in foreground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(intent)
                else
                    context.startService(intent)
                true
            } else {
                //cancels service
                context.stopService(intent)

                false
            }
        }

        //returns if server is currently in foreground
        @JvmField
        var isServiceRunningInForeground: Boolean = false
    }

    /**Prevents multiple instances of loading thread
     * When new timetable is loaded from server, this service is started
     * And this service also can load timetable from server, which leads
     * into infinite cycle with out this protection*/
    private var startAble = true

    override fun onCreate() {
        super.onCreate()
        //sets up with default notification
        startForeground(NOTIFICATION_ID, waitingNotificationContent())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //sets up with default notification
        startForeground(NOTIFICATION_ID, waitingNotificationContent())
        isServiceRunningInForeground = true

        Log.i(TAG, "Timetable service started")

        //prevents more instances
        if (startAble) {
            Log.i(TAG, "Updating notification with data")
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                getNotifications()
            }
        }

        setEverydaySetup()

        return START_NOT_STICKY
    }

    private suspend fun getNotifications() {
        //locks method
        startAble = false
        try {
            todo()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        startAble = true
    }

    private suspend fun todo() {
        //today
        val cal = TimeTools.today
        var week =
            TimetableLoader.loadFromStorage(
                cal
            )

        //tries to load week from server
        if (week == null) {
            if (CheckInternet.canUseInternet() && CheckInternet.check()) {

                week =
                    TimetableLoader.loadFromServer(
                        cal
                    )

                if (week == null) {
                    MyToast.makeText(
                        this@TTNotifyService,
                        R.string.error_cannot_download_timetable,
                        Toast.LENGTH_LONG
                    ).show()

                    Log.i(TAG, "Cannot download timetable")
                    //error messages
                    replaceNotification(
                        generateNotification(
                            "Cannot download timetable",
                            "Please connect to internet"
                        )
                    )

                    //stops foreground service
                    stopForeground(false)
                    isServiceRunningInForeground = false
                    return
                }
            } else {
                MyToast.makeText(
                    this@TTNotifyService,
                    R.string.error_no_timetable,
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }
        Log.i(TAG, "Week loaded")

        val today = week.today()
        if ((today != null) && !today.isEmpty()) {

            //gets selected notification texts
            val messages = generateNotificationContent(week)
            if (messages != null) {
                val title = messages[0]
                val subtitle = messages[1]

                Log.i(TAG, "Starting foreground")
                //updates notification
                replaceNotification(generateNotification(title, subtitle))

                return
            }
        }
        //stops on error
        Log.i(TAG, "Stopping foreground")
        stopForeground(true)
        isServiceRunningInForeground = false


    }

    /**@return new notification object with inputted texts*/
    private fun generateNotification(title: String, subtitle: String): Notification {
        //opens timetable in MainActivity on click
        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.mobile_navigation)
            .setDestination(R.id.nav_timetable)
            //.setArguments(bundle)
            .createPendingIntent()

        //generates Notification object
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(this,
                NOTIFICATION_CHANEL_ID
            )
                .setContentTitle(title)
                .setContentText(subtitle)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.nav_timetable)
                .setOnlyAlertOnce(true)
                .setStyle(Notification.BigTextStyle())
            builder.build()
        } else {
            val builder = NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.nav_timetable)
                .setStyle(NotificationCompat.BigTextStyle())
            builder.build()
        }
    }

    /**Puts notification into notification area*/
    private fun replaceNotification(notification: Notification) {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**@return texts to be putted into notification*/
    private fun generateNotificationContent(week: Week): Array<String>? {

        //now in seconds since midnight
        val now = TimeTools.timeToSeconds(
            TimeTools.now.toLocalTime()
        )

        //strings generated in NotificationContent
        val actions = NotificationContent(
            this
        ).generateActions(week) ?: return null

        //times
        val keys = ArrayList<Int>(actions.keys)
        keys.sort()

        //prints entries for debugging
        /*Log.d(TAG, "$now\t now")
        keys.forEach {
            if (actions[it] != null)
                Log.d(
                    TAG, "$it\t "
                            + "${floor(it / (3600.0)).toInt() + 1}:${floor(it / 60.0).toInt() % 60} "
                            + "${actions[it]!![0]} - ${actions[it]!![1]}"
                )
            else
                Log.d(
                    TAG, "$it\t"
                            + "${floor(it / (3600.0)).toInt() + 1}:${floor(it / 60.0).toInt() % 60} "
                            + "null"
                )
        }*/

        //selects current texts
        for (it in keys) {
            if (it > now) {
                val cal = TimeTools.today
                    .with(ChronoField.HOUR_OF_DAY, floor(it / (60.0 * 60.0)).toLong())
                    .with(ChronoField.MINUTE_OF_HOUR, floor(it / 60.0).toLong() % 60)
                setDayAlarm(cal)
                return actions[it]
            }
        }

        return null
    }

    /**@return notification containing texts while the oder ones are loading*/
    private fun waitingNotificationContent(): Notification {
        return generateNotification(
            getString(R.string.preparing_timetable),
            getString(R.string.should_not_take_long)
        )
    }

    /**Sets pending intent into AlarmManager which updated data during day*/
    private fun setDayAlarm(time: ZonedDateTime) {
        //val intent = Intent(PENDING_INTENT_TRIGGER_NAME)

        val intent = Intent(this, TTReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            TTReceiver.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        setAlarm(pendingIntent, time)
    }

    /**Updates every morning and init new date*/
    private fun setEverydaySetup() {
        //val intent = Intent(PENDING_INTENT_TRIGGER_NAME)

        val intent = Intent(this, TTReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            TTReceiver.REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val time = ZonedDateTime.of(
            LocalDate.now().plusDays(1),
            LocalTime.MIDNIGHT.withHour(5),
            TimeTools.CET
        )

        setRepeatingAlarm(pendingIntent, time)

    }

    /**Puts Pending intent into AlarmManager as exact*/
    private fun setAlarm(pendingIntent: PendingIntent, time: ZonedDateTime) {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

    /**Puts Pending intent into AlarmManager as repeating*/
    private fun setRepeatingAlarm(pendingIntent: PendingIntent, time: ZonedDateTime) {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        when {
            Build.VERSION.SDK_INT >= 19 -> {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC,
                    time.toInstant().toEpochMilli(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC,
                    AlarmManager.INTERVAL_DAY,
                    time.toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
        }
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
