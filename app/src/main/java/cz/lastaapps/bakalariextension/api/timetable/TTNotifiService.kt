package cz.lastaapps.bakalariextension.api.timetable

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MyToast
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.ChronoUnit
import kotlin.math.floor


class TTNotifiService : Service() {

    companion object {
        private val TAG = TTNotifiService::class.java.simpleName
        const val NOTIFICATION_CHANEL_ID = "timetable_notification_service"
        private const val NOTIFICATION_ID = 1

        fun startService(context: Context = App.context) {
            Handler(Looper.getMainLooper()).post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(Intent(context, TTNotifiService::class.java))
                else
                    context.startService(Intent(context, TTNotifiService::class.java))
            }
        }
    }

    /**Prevents multiple instances of loading thread
     * When new timetable is loaded from server, this service is started
     * And this service also can load timetable from server, witch leads
     * into infinite cycle with out this protection*/
    private var startAble = true

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, waitingNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(NOTIFICATION_ID, waitingNotification())

        Log.i(TAG, "Timetable service started")

        if (startAble) {
            Log.i(TAG, "Updating notification with data")
            Thread(todo).start()
        }

        setRegularRepeating()

        return START_NOT_STICKY
    }

    private val todo = object : Runnable {

        override fun run() {
            startAble = false
            try {
                todo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            startAble = true
        }

        fun todo() {
            val cal = TimeTools.cal
            var week = Timetable.loadFromStorage(cal)

            if (week == null) {
                if (CheckInternet.canUseInternet() && CheckInternet.check()) {
                    week = Timetable.loadFromServer(cal)
                    if (week == null) {
                        MyToast.makeText(
                            this@TTNotifiService,
                            R.string.error_cannot_download_timetable,
                            Toast.LENGTH_LONG
                        ).show()

                        Log.i(TAG, "Cannot download timetable")
                        updateNotification(
                            generateNotification(
                                "Cannot download timetable",
                                "Please connect to internet"
                            )
                        )
                        stopForeground(false)
                        return
                    }
                } else {
                    MyToast.makeText(
                        this@TTNotifiService,
                        R.string.error_no_timetable,
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }
            Log.i(TAG, "Week loaded")

            val today = week.today()
            if ((today != null) && !today.isEmpty()) {

                val messages = generateNotificationData(week)
                if (messages != null) {
                    val title = messages[0]
                    val subtitle = messages[1]

                    Log.i(TAG, "Starting foreground")
                    updateNotification(generateNotification(title, subtitle))

                    return
                }
            }
            Log.i(TAG, "Stopping foreground")
            stopForeground(true)
        }

    }

    private fun generateNotificationData(week: Week): Array<String>? {

        val now = TimeTools.calToSeconds(
            TimeTools.now.toLocalTime())

        val actions = NotificationContent.generateActions(week) ?: return null

        val keys = ArrayList<Int>(actions.keys)
        keys.sort()

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

        for (it in keys) {
            if (it > now) {
                val cal = TimeTools.cal
                    .with(ChronoField.HOUR_OF_DAY, floor(it / (60.0 * 60.0)).toLong())
                    .with(ChronoField.MINUTE_OF_HOUR, floor(it / 60.0).toLong() % 60)
                setNextAlarm(cal)
                return actions[it]
            }
        }

        return null
    }

    private fun updateNotification(notification: Notification) {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun generateNotification(title: String, subtitle: String): Notification {
        val pendingIntent = NavDeepLinkBuilder(this)
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.nav_timetable)
                //.setArguments(bundle)
                .createPendingIntent()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(this, NOTIFICATION_CHANEL_ID)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.nav_timetable)
                .setOnlyAlertOnce(true)
            builder.build()
        } else {
            val builder = NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.nav_timetable)
            builder.build()
        }
    }

    private fun waitingNotification(): Notification {
        return generateNotification(
            getString(R.string.preparing_timetable),
            getString(R.string.should_not_take_long)
        )
    }

    private fun setNextAlarm(cal: ZonedDateTime) {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TTReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            TTReceiver.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(
                AlarmManager.RTC,
                cal.toInstant().toEpochMilli(),
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC,
                cal.toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }

    private fun setRegularRepeating() {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TTReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            TTReceiver.REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cal = ZonedDateTime.now(ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.DAYS)
            .plusDays(1)
            .with(ChronoField.HOUR_OF_DAY, 4)

        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            cal.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
