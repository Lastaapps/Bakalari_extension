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

package cz.lastaapps.bakalariextension.login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.api.homework.HomeworkLoader
import cz.lastaapps.bakalariextension.api.marks.MarksLoader
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.receivers.BootReceiver
import cz.lastaapps.bakalariextension.receivers.TimeChangeReceiver
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.services.timetablenotification.TTReceiver
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.attachment.AttachmentDownload
import cz.lastaapps.bakalariextension.ui.timetable.small.widget.SmallTimetableWidget

/**Run when user logs in*/
class OnLogin {

    companion object {
        private val TAG = OnLogin::class.java.simpleName

        //is called from outside
        /**Does on login init
         * @return if background part succeed*/
        fun onLogin(context: Context): Boolean {
            Handler(Looper.getMainLooper()).post {
                Log.e(TAG, "Running foreground")
                foreground(context)
            }
            Log.e(TAG, "Running background")

            val success = background(context)

            Log.e(TAG, "success $success")
            //init failed, logs out
            if (!success)
                Logout.logout()

            return success
        }

        /**Runs init in foreground*/
        private fun foreground(context: Context) {

        }

        /**Runs init in background*/
        private fun background(context: Context): Boolean {

            //inits notification channels
            initNotificationChannels(context)

            //enables receivers and services
            val receivers = arrayOf(
                BootReceiver::class.java,
                TimeChangeReceiver::class.java,
                TTReceiver::class.java,
                SmallTimetableWidget::class.java
            )
            for (receiver in receivers) {
                val pm: PackageManager = context.packageManager
                val component = ComponentName(context, receiver)

                pm.setComponentEnabledSetting(
                    component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            //download default user data
            if (!User.download())
                return false

            //downloads some basic data for offline use if user is connected to wifi
            if (!CheckInternet.connectedMobileData()) {
                //download timetables
                Log.i(TAG, "Downloading default timetables")
                val date = TimeTools.monday
                //what timetables should be loaded
                val array = arrayOf(
                    TimeTools.PERMANENT,
                    date,
                    TimeTools.previousWeek(date),
                    TimeTools.nextWeek(date)
                )

                for (it in array)
                    TimetableLoader.loadFromServer(it)


                //downloads marks
                MarksLoader.loadFromServer()

                //download homework
                HomeworkLoader.loadFromServer()
            }

            return true
        }

        /**inits notification channels*/
        private fun initNotificationChannels(context: Context) {
            //Timetable notify service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //Timetable notification
                {
                    val name =
                        context.getString(R.string.timetable_chanel_name)
                    val descriptionText =
                        context.getString(R.string.timetable_chanel_description)
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val mChannel = NotificationChannel(
                        TTNotifyService.NOTIFICATION_CHANEL_ID,
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
                        context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
                    //notificationManager.deleteNotificationChannel(mChannel.id)
                    notificationManager.createNotificationChannel(mChannel)
                }.invoke();

                //Download attachment notification
                {
                    val name =
                        context.getString(R.string.attachment_downloading_chanel_name)
                    val descriptionText =
                        context.getString(R.string.attachment_downloading_chanel_description)

                    val mChannel = NotificationChannel(
                        AttachmentDownload.ATTACHMENT_DOWNLOAD_CHANEL,
                        name,
                        NotificationManager.IMPORTANCE_LOW
                    )
                    mChannel.description = descriptionText
                    mChannel.setShowBadge(true)
                    mChannel.setSound(null, null)
                    mChannel.enableVibration(false)
                    mChannel.enableLights(false)

                    val notificationManager =
                        context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(mChannel)
                }.invoke();
            }
        }
    }
}