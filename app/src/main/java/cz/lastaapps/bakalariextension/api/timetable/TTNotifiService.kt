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
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MyToast
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.floor


class TTNotifiService : Service() {

    companion object {
        private val TAG = TTNotifiService::class.java.simpleName
        const val NOTIFICATION_CHANEL_ID = "timetable_notification_service"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, waitingNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(NOTIFICATION_ID, waitingNotification())

        Log.i(TAG, "Timetable service started started")
        Thread(todo).start()

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

        val cal = Calendar.getInstance()
        val now = TTTools.calToSeconds(cal)

        val actions = generateActions(week) ?: return null

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
                return actions[it] ?: null
            }
        }
        return null
    }

    private fun generateActions(week: Week): HashMap<Int, Array<String>?>? {
        val actions = HashMap<Int, Array<String>?>()

        val day = week.getDay(TTTools.cal)
        val patterns = week.patterns
        if (day == null)
            return null

        val firstLesson = day.firstLessonIndex()
        val lastLesson = (day.lastLessonIndex() + if (day.endsWithLunch()) 1 else 0)
            .coerceAtMost(day.lessons.size - 1)

        if (firstLesson < 0 || lastLesson < 0)
            return null

        for (index in firstLesson..lastLesson) {

            val pattern = patterns[index]
            val lesson = day.getLesson(index)
            val nextPattern = if (index != lastLesson) patterns[index + 1] else null
            val nextLesson = if (index != lastLesson) day.getLesson(index + 1) else null

            val begin = TTTools.calToSeconds(TTTools.parseTime(pattern.begin, TTTools.CET))
            val end = TTTools.calToSeconds(TTTools.parseTime(pattern.end, TTTools.CET))

            val nextStr = getString(R.string.next)
            val endStr = getString(R.string.end)
            val breakStr = getString(R.string.interuption)
            val untilStr = getString(R.string.until)
            val groupStr = getString(R.string.group)
            val freeLessonStr = getString(R.string.free_lesson)

            //silent zone before start
            if (index == firstLesson) {
                actions[begin - (60 * 60)] = null
            }

            // @formatter:off
            //normal lesson
            if (lesson.isNormal()) {

                //before lesson starts
                actions[begin] = arrayOf(
                    "${pattern.begin} ${lesson.roomShortcut} - ${lesson.subject}",
                    "${lesson.teacher}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${lesson.groupShortcut}"
                )

                if (nextLesson == null || nextPattern == null) {
                    //the last lesson of the day

                    //during lesson
                    actions[end - 10 * 60] = arrayOf(
                        "${lesson.roomShortcut} - ${lesson.subject}",
                        "$endStr ${pattern.end}"
                    )

                    //last 10 minutes of a lesson
                    actions[end] = arrayOf(
                        "$endStr ${pattern.end}",
                        getString(R.string.have_nice_day)
                    )

                }
                else if (nextLesson.isNormal()) {
                    //before normal lesson

                    //during lesson
                    actions[end - 10 * 60] = arrayOf(
                        "${lesson.roomShortcut} - ${lesson.subject}",
                        "$endStr ${pattern.end}, $nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}"
                    )

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}",
                        "$breakStr ${pattern.end} - ${nextPattern.begin}"
                    )

                }
                else if (nextLesson.isFree()) {
                    //before free lesson

                    //during lesson
                    actions[end - 10 * 60] = arrayOf(
                        "${lesson.roomShortcut} - ${lesson.subject}",
                        "$endStr ${pattern.end}, $nextStr: $freeLessonStr"
                    )

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}",
                        "$endStr ${pattern.end}"
                    )
                }
                else if (nextLesson.isAbsence()) {
                    //before free lesson

                    //during lesson
                    actions[end - 10 * 60] = arrayOf(
                        "${lesson.roomShortcut} - ${lesson.subject}",
                        "$endStr ${pattern.end}, $nextStr: ${lesson.shortcut} ${lesson.name}"
                    )

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$nextStr: ${lesson.shortcut} ${lesson.name}",
                        "$endStr ${pattern.end}"
                    )
                }
            }
            else if (lesson.isFree()) {
                //lunch lesson

                if (nextLesson == null || nextPattern == null) {
                    //the last lesson - free - probably lunch

                    //before lesson starts
                    //is same as before lesson end
                    //actions[begin] = arrayOf("", "")

                    //during lesson
                    //is same as before lesson end
                    //actions[end - 10 * 60] = arrayOf("", "")

                    //last 10 minutes of a lesson
                    actions[end] = arrayOf(
                        "$freeLessonStr",
                        "${getString(R.string.finally_home)}"
                    )

                }
                else if (nextLesson.isNormal()) {
                    //before normal lesson

                    //before lesson starts
                    //same as during free lesson
                    //actions[begin] = arrayOf("", "")

                    //during lesson
                    actions[end - 10 * 60] = arrayOf(
                        "$freeLessonStr $untilStr ${pattern.end}",
                        "$nextStr: ${nextPattern.begin} ${nextLesson.roomShortcut} - ${nextLesson.subject}"
                    )

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}",
                        "$breakStr ${pattern.end} - ${nextPattern.begin}"
                    )

                }
                else if (nextLesson.isFree()) {
                    //before free lesson

                    //before lesson starts
                    //same whole time
                    //actions[begin] = arrayOf("", "")

                    //during lesson
                    //same whole time
                    //actions[end - 10 * 60] = arrayOf("", "")

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$freeLessonStr",
                        "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}"
                    )
                }
                else if (nextLesson.isAbsence()) {
                    //before free lesson

                    //during lesson

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$freeLessonStr $untilStr ${pattern.end}",
                        "$nextStr: ${nextPattern.begin} ${lesson.shortcut} ${lesson.name}"
                    )
                }
            }
            else if (lesson.isAbsence()) {
                //if is class absence

                //before lesson starts
                actions[begin] = arrayOf(
                    "${pattern.begin} ${lesson.shortcut}",
                    "${lesson.name}"
                )

                if (nextLesson == null || nextPattern == null) {
                    //the last lesson - free - probably lunch

                    //before lesson starts
                    //is same as before lesson end
                    //actions[begin] = arrayOf("", "")

                    //during lesson
                    //is same as before lesson end
                    //actions[end - 10 * 60] = arrayOf("", "")

                    //last 10 minutes of a lesson
                    actions[end] = arrayOf(
                        "${lesson.shortcut} ${lesson.name}",
                        "${getString(R.string.finally_home)}"
                    )

                }
                else if (nextLesson.isNormal()) {
                    //before normal lesson

                    //before lesson starts
                    //same as during free lesson
                    //actions[begin] = arrayOf("", "")

                    //during lesson
                    actions[end - 10 * 60] = arrayOf(
                        "${lesson.shortcut} ${lesson.name}",
                        "$endStr: ${pattern.end}, $nextStr: ${nextPattern.begin} ${nextLesson.roomShortcut} - ${nextLesson.subject}"
                    )

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "$nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}",
                        "$breakStr ${pattern.end} - ${nextPattern.begin}"
                    )

                }
                else if (nextLesson.isFree()) {
                    //before free lesson

                    //before lesson starts
                    //same whole time
                    //actions[begin] = arrayOf("", "")

                    //during lesson
                    //same whole time
                    //actions[end - 10 * 60] = arrayOf("", "")

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "${lesson.shortcut} ${lesson.name}",
                        "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}"
                    )
                }
                else if (nextLesson.isAbsence()) {
                    //before free lesson

                    //during lesson

                    //last 10 minutes of the lesson
                    actions[end] = arrayOf(
                        "${lesson.shortcut} ${lesson.name}",
                        "$nextStr: ${lesson.shortcut} ${lesson.name}"
                    )
                }
            }
            // @formatter:on
        }
        return actions
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
                .setSmallIcon(R.drawable.icon)
                .setOnlyAlertOnce(true)
            builder.build()
        } else {
            val builder = NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.icon)
            builder.build()
        }
    }

    private fun waitingNotification() : Notification {
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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
