package cz.lastaapps.bakalariextension.api.timetable

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import cz.lastaapps.bakalariextension.LoadingActivity
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MyToast
import java.util.*
import kotlin.collections.ArrayList
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

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, waitingNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(NOTIFICATION_ID, waitingNotification())

        Log.i(TAG, "Timetable service started started")
        Thread(todo).start()

        setRegularRepeating()

        return START_NOT_STICKY
    }

    private val todo = object : Runnable {

        override fun run() {

            val cal = TTTools.cal
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

                    Handler(Looper.getMainLooper()).post {
                        Log.i(TAG, "Starting foreground")
                        updateNotification(generateNotification(title, subtitle))
                    }
                    return
                }
            }
            Handler(Looper.getMainLooper()).post {
                Log.i(TAG, "Stopping foreground")
                stopForeground(true)
            }
        }

    }

    private fun generateNotificationData(week: Week): Array<String>? {

        val now = TTTools.calToSeconds(TTTools.now)

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
                val cal = TTTools.cal
                cal.set(Calendar.HOUR_OF_DAY, floor(it / (60.0 * 60.0)).toInt())
                cal.set(Calendar.MINUTE, floor(it / 60.0).toInt() % 60)
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
        val pendingIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, LoadingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val bundle = Bundle()
                bundle.putInt(MainActivity.NAVIGATE, R.id.nav_timetable)
                putExtras(bundle)
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

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

    private fun setNextAlarm(cal: Calendar) {
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
                cal.time.time,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC,
                cal.time.time,
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

        val cal = TTTools.cal
        cal.set(Calendar.HOUR_OF_DAY, 4)
        cal.time = Date(cal.timeInMillis + TTTools.DAY)
        setNextAlarm(cal)

        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            cal.time.time,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
