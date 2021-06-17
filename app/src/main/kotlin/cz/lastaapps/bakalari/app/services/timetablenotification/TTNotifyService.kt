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

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.NavDirections
import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.api.entity.timetable.Week
import cz.lastaapps.bakalari.api.repo.core.APIRepo
import cz.lastaapps.bakalari.api.repo.timetable.timetableRepository
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.navigation.ComplexDeepLinkNavigator
import cz.lastaapps.bakalari.app.ui.start.loading.LoadingFragmentDirections
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.App
import cz.lastaapps.bakalari.platform.BaseService
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import cz.lastaapps.bakalari.tools.TimeTools.toDaySeconds
import cz.lastaapps.bakalari.tools.normalizeID
import cz.lastaapps.bakalari.tools.startForegroundServiceCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor

/**Shows notification generated in NotificationContent*/
class TTNotifyService : BaseService() {

    companion object {
        private val TAG get() = TTNotifyService::class.java.simpleName
        private val NOTIFICATION_ID =
            R.id.notification_id_timetable_notification_service.normalizeID()

        /**used for starting service statically, can also stop service if it is disabled
         * @return if service was started or canceled*/
        fun startService(context: Context = App.context): Boolean {

            val intent = Intent(context, TTNotifyService::class.java)

            return if (MySettings.withAppContext().timetableNotificationAccountUUID != null) {
                Log.i(TAG, "Starting service")

                //starts service in foreground
                context.startForegroundServiceCompat(intent)

                true
            } else {
                //cancels service
                Log.i(TAG, "Canceling service")

                context.stopService(intent)

                false
            }
        }

        //returns if server is currently in foreground
        @JvmField
        var isServiceRunningInForeground: Boolean = false
    }

    private val NOTIFICATION_CHANEL_ID by lazy { getString(R.string.channel_timetable_id) }

    private lateinit var weekStateFlow: MutableStateFlow<Week>

    override fun onCreate() {
        super.onCreate()

        initNotificationChannel()

        //sets up with default notification
        startForeground(NOTIFICATION_ID, waitingNotificationContent())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //sets up with default notification
        startForeground(NOTIFICATION_ID, waitingNotificationContent())
        isServiceRunningInForeground = true

        Log.i(TAG, "Timetable service started")

        lifecycleScope.launch(Dispatchers.Default) {

            Log.i(TAG, "Updating notification with data")

            try {
                val uuid = obtainUserId()

                if (uuid != null) {
                    observeForTimetableUpdate(uuid)

                    if (this@TTNotifyService::weekStateFlow.isInitialized) {
                        update()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        lifecycleScope.launch(Dispatchers.Default) {
            setEverydaySetup()
        }

        return START_NOT_STICKY
    }

    /**Obtains valid user uuid or returns null (also updates notification if user had been deleted)*/
    private suspend fun obtainUserId(): UUID? {
        val uuid = MySettings.withAppContext().timetableNotificationAccountUUID

        if (uuid == null) {
            stopForeground(true)
            return null
        }

        val accountRepo = AccountsDatabase.getDatabase(this).repository
        if (!accountRepo.exitsUUID(uuid)) {
            val settingsPendingIntent = NavDeepLinkBuilder(this)
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.navigation)
                .setDestination(R.id.nav_settings_root)
                //.setArguments(bundle)
                .createPendingIntent()

            replaceNotification(
                generateNotification(
                    getString(R.string.timetable_notification_user_deleted),
                    getString(R.string.timetable_notification_open_settings),
                    settingsPendingIntent,
                )
            )
            stopForeground(false)
        }
        return uuid
    }

    private var observerUUID: UUID? = null
    private var collectionJob: Job? = null

    /**Starts the observation of repo data*/
    private suspend fun observeForTimetableUpdate(uuid: UUID) = coroutineScope {

        observerUUID = uuid

        //cancels previous job
        collectionJob?.cancel()

        //loads parameters
        val date = TimeTools.today.toCzechDate()
        val database = APIBase.getDatabase(this@TTNotifyService, uuid)!!
        val repo = database.timetableRepository.getRepositoryForDate(date)
        val shared = repo.getWeekForDate(date)
            .shareIn(this, SharingStarted.WhileSubscribed(), 1)

        //starts a new observation
        collectionJob = launch {

            //if null timetable has been already observed
            var nullSeen = false

            shared.collect {

                //data failed to load the first time
                if (it == null && !nullSeen) {

                    nullSeen = true

                    //tries to download data
                    val refreshFlow = repo.refreshData()
                    while (true) {
                        when (refreshFlow.first()) {
                            APIRepo.LOADING -> continue
                            APIRepo.FAILED -> {
                                onDownloadFailed()
                                break
                            }
                            APIRepo.SUCCEEDED -> break
                        }
                    }

                } else if (it == null && nullSeen) { //data cannot be obtained
                    onDownloadFailed()

                } else { //data obtained

                    if (!this@TTNotifyService::weekStateFlow.isInitialized)
                        weekStateFlow = MutableStateFlow(it!!)
                    else
                        weekStateFlow.value = it!!

                    launch { update() }
                }
            }
        }
    }

    /** Processes the new data and updates notification data*/
    private fun update() {
        val week = weekStateFlow.value

        Log.i(TAG, "Week updated")

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

        //stops on error or weekend/holiday
        Log.i(TAG, "Stopping foreground")
        stopForeground(true)
        isServiceRunningInForeground = false
    }

    /**Called when all attempts to download timetable failed*/
    private fun onDownloadFailed() {

        Log.i(TAG, "Cannot download timetable")

        //during school year shows notification with the problem, during holiday there can be
        //no timetable to download, so the message would show every day
        if (ZonedDateTime.now().monthValue !in 7..8) {
            //error messages
            replaceNotification(
                generateNotification(
                    getString(R.string.timetable_notification_cannot_download),
                    getString(R.string.timetable_notification_connect_to_internet),
                )
            )

            //stops foreground service
            stopForeground(false)
        } else {
            //removes notification during the summer holiday
            stopForeground(true)
        }

        isServiceRunningInForeground = false
    }

    /**@return new notification object with inputted texts*/
    private fun generateNotification(
        title: CharSequence,
        subtitle: CharSequence,
        pendingIntent: PendingIntent? = getTimetableOpenPendingIntent()
    ): Notification {

        val builder = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANEL_ID
        )
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.nav_timetable)
            .setStyle(NotificationCompat.BigTextStyle())
        return builder.build()
    }

    /**Puts notification into notification area*/
    private fun replaceNotification(notification: Notification) {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**@return texts to be putted into notification*/
    private fun generateNotificationContent(week: Week): Array<CharSequence>? {

        //now in seconds since midnight
        val now = TimeTools.now.toLocalTime().toDaySeconds()

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
                val time = TimeTools.today
                    .with(ChronoField.HOUR_OF_DAY, floor(it / (60.0 * 60.0)).toLong())
                    .with(ChronoField.MINUTE_OF_HOUR, floor(it / 60.0).toLong() % 60)

                setDayAlarm(time)

                return actions[it]
            }
        }

        return null
    }

    /**@return notification containing texts while the oder ones are loading*/
    private fun waitingNotificationContent(): Notification {
        return generateNotification(
            getString(R.string.timetable_preparing),
            getString(R.string.timetable_should_not_take_long)
        )
    }

    /**@return Pending intent, which opens timetable*/
    private fun getTimetableOpenPendingIntent(): PendingIntent? {
        val uuid = observerUUID ?: return null

        val intent = ComplexDeepLinkNavigator.createIntent(this, MainActivity::class.java, listOf(
            LoadingFragmentDirections.actionLoadingToUser(uuid),
            object : NavDirections {
                override fun getActionId(): Int = R.id.nav_timetable
                override fun getArguments(): Bundle = Bundle()
            }
        ))

        return PendingIntent.getActivity(
            this,
            R.id.request_code_tt_timetable_open_pending_intent.normalizeID(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
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
                    time.toInstant().toEpochMilli(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_timetable_name)
            val descriptionText = getString(R.string.channel_timetable_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                name,
                importance
            )
            mChannel.description = descriptionText
            mChannel.setShowBadge(false)
            mChannel.setSound(null, null)
            mChannel.enableVibration(false)
            mChannel.enableLights(false)
            mChannel.importance = NotificationManager.IMPORTANCE_HIGH

            val notificationManager =
                getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            //notificationManager.deleteNotificationChannel(mChannel.id)
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}
