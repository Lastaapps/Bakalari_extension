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

package cz.lastaapps.bakalari.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.lastaapps.bakalari.app.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalari.app.widgets.smalltimetable.SmallTimetableWidget
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**Receives when user changes time - refreshes services and widgets and makes access token expired*/
class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private val TAG get() = TimeChangeReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED) {
            Log.i(TAG, "Time changed, updating timetable notification")

            //updates services and widgets
            TTNotifyService.startService(context)
            SmallTimetableWidget.update(context)

            val holder = goAsync()

            //makes access token expired
            CoroutineScope(Dispatchers.Default).launch {
                AccountsDatabase.getDatabase(context.applicationContext).repository.expireTokens()
                holder.finish()
            }
        }
    }
}
