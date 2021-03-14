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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.app.api.database.APIRepo
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings

class LoginImpl(val context: Context, val account: BakalariAccount) {

    companion object {
        private val TAG = LogoutImpl::class.simpleName
    }

    private val ATTACHMENT_CHANNEL_ID =
        context.getString(R.string.channel_attachment_downloading_id)

    private val notMgr = NotificationManagerCompat.from(context)

    suspend fun doLogin(): Boolean = try {

        addToDatabase()

        loadUser()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            initChannels()

        setInTimetableNotification()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    suspend fun addToDatabase() {
        val repo = AccountsDatabase.getDatabase(context).repository

        repo.addAccount(context, account)
    }

    suspend fun loadUser() {
        //download default user data
        when (APIBase.getDatabase(context, account).userRepository.refreshDataAndWait()) {
            APIRepo.FAILED -> throw Exception("Failed to download the user object")
        }
    }

    suspend fun initChannels() {
        createChannelGroup()

        createAttachmentChannel()
    }

    fun createChannelGroup() {
        val group = NotificationChannelGroupCompat.Builder(groupId)
            .setName(account.profileName)
            .build()

        notMgr.createNotificationChannelGroup(group)
    }

    fun createAttachmentChannel() {
        val name =
            context.getString(R.string.channel_attachment_downloading_name)
        val description =
            context.getString(R.string.channel_attachment_downloading_description)

        val mChannel = NotificationChannelCompat.Builder(
            createChannelId(ATTACHMENT_CHANNEL_ID),
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(name)
            .setDescription(description)
            .setGroup(groupId)
            .setShowBadge(true)
            .setSound(null, null)
            .setVibrationEnabled(true)
            .setLightsEnabled(true)
            .build()

        notMgr.createNotificationChannel(mChannel)
    }

    fun setInTimetableNotification() {
        val sett = MySettings.withAppContext()
        if (sett.timetableNotificationAccountUUID == null)
            sett.timetableNotificationAccountUUID = account.uuid
    }

    private val groupId = getGroupId(account.uuid)
    private fun createChannelId(id: String) =
        getChannelId(account.uuid, id)
}
