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

package cz.lastaapps.bakalari.app.ui.start.login.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.app.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.app.widgets.smalltimetable.SmallTimetableWidget
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogoutImpl(val context: Context, val account: BakalariAccount) {

    companion object {
        private val TAG get() = LogoutImpl::class.simpleName
    }

    private val notMgr = NotificationManagerCompat.from(context)

    suspend fun doLogout() {

        Log.i(TAG, "Doing log out")
        deleteDatabase()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            removeChannels()
    }

    suspend fun deleteDatabase() {
        Log.v(TAG, "Deleting database")

        if (CurrentUser.accountUUID.value == account.uuid) {
            CurrentUser.releaseDatabase()
        }
        APIBase.deleteDatabase(context, account)
    }

    suspend fun removeChannels() {
        Log.v(TAG, "Removing notification channels")

        notMgr.deleteNotificationChannelGroup(groupId)
    }

    suspend fun updateServices() {
        withContext(Dispatchers.Main) {
            Log.v(TAG, "Updating services and widgets")
            TTNotifyService.startService(context)
            SmallTimetableWidget.update(context)
        }
    }

    private val groupId = getGroupId(account.uuid)
}