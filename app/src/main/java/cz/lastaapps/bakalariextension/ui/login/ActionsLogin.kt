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

package cz.lastaapps.bakalariextension.ui.login

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.absence.AbsenceLoader
import cz.lastaapps.bakalariextension.api.events.EventsLoader
import cz.lastaapps.bakalariextension.api.homework.HomeworkLoader
import cz.lastaapps.bakalariextension.api.marks.MarksLoader
import cz.lastaapps.bakalariextension.api.subjects.SubjectLoader
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.user.UserLoader
import cz.lastaapps.bakalariextension.receivers.BootReceiver
import cz.lastaapps.bakalariextension.receivers.TimeChangeReceiver
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.services.timetablenotification.TTReceiver
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.attachment.AttachmentDownload
import cz.lastaapps.bakalariextension.widgets.smalltimetable.SmallTimetableWidget

/**Run when user logs in*/
class ActionsLogin {

    companion object {
        private val TAG = ActionsLogin::class.java.simpleName

        //is called from outside
        /**Does on login init
         * @return if background part succeed*/
        suspend fun onLogin(context: Context): Boolean {
            Log.i(TAG, "Running background")

            val success = background(context)

            Log.i(TAG, "success $success")
            //init failed, logs out
            if (!success)
                ActionsLogout.logout()

            return success
        }

        /**Runs in background*/
        private suspend fun background(context: Context): Boolean {

            //inits notification channels
            initNotificationChannels(context)

            //download default user data
            if (UserLoader.loadFromServer() == null)
                return false

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

            //downloads some basic data for offline use if user is connected to wifi
            if (!CheckInternet.connectedMobileData()) {
                Log.i(TAG, "Downloading default data")

                //download timetables
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

                //downloads subjects
                SubjectLoader.loadFromServer()

                //downloads absence
                AbsenceLoader.loadFromServer()

                //downlands events
                EventsLoader.loadFromServer()

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
                        context.getString(R.string.chanel_timetable_name)
                    val descriptionText =
                        context.getString(R.string.chanel_timetable_description)
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
                        context.getString(R.string.chanel_attachment_downloading_name)
                    val descriptionText =
                        context.getString(R.string.chanel_attachment_downloading_description)

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
                }.invoke()
            }
        }
    }
}