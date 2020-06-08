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

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.api.homework.HomeworkStorage
import cz.lastaapps.bakalariextension.api.marks.MarksStorage
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.receivers.BootReceiver
import cz.lastaapps.bakalariextension.receivers.TimeChangeReceiver
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.services.timetablenotification.TTReceiver
import cz.lastaapps.bakalariextension.widgets.smalltimetable.SmallTimetableWidget

/**Deletes saved tokens and all oder data, then restarts app*/
class Logout {

    companion object {
        private val TAG = Logout::class.java.simpleName

        fun logout() {
            Log.i(TAG, "Login out")

            //stops services
            App.context.stopService(Intent(App.context, TTNotifyService::class.java))

            //deletes login info - school name, url and username remains
            LoginData.accessToken = ""
            LoginData.refreshToken = ""
            LoginData.tokenExpiration = 0

            User.clear()

            //deletes timetables
            TTStorage.deleteAll()

            //deletes marks
            MarksStorage.delete()

            //deletes homework
            HomeworkStorage.delete()

            //disables receivers
            val receivers = arrayOf(
                BootReceiver::class.java,
                TimeChangeReceiver::class.java,
                TTReceiver::class.java,
                SmallTimetableWidget::class.java
            )
            for (receiver in receivers) {
                val pm: PackageManager = App.context.packageManager
                val component = ComponentName(App.context, receiver)

                pm.setComponentEnabledSetting(
                    component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            Log.i(TAG, "Logged out")
        }
    }
}