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

package cz.lastaapps.bakalari.platform

import android.app.Notification
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**Starts foreground service and automatically creates a notification*/
abstract class BaseForegroundService : BaseService() {

    /**id of the notification channel*/
    private val channelId by lazy { getString(R.string.channel_general_id) }

    /**The id for the service notification*/
    protected abstract val notificationId: Int

    /**The message to be shown in the notification*/
    protected abstract val notificationMessage: String

    override fun onCreate() {
        super.onCreate()

        initNotificationChannel()
        startForeground(notificationId, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId).also {
            startForeground(notificationId, createNotification())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    /**Creates general notification channel*/
    private fun initNotificationChannel() {
        val mChannel = NotificationChannelCompat.Builder(
            channelId,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.channel_general_name))
            .setDescription(getString(R.string.channel_general_description))
            .setShowBadge(true)
            .setSound(null, null)
            .setVibrationEnabled(false)
            .setLightsEnabled(false)
            .build()

        NotificationManagerCompat.from(this).createNotificationChannel(mChannel)
    }

    /**Creates notification with the text given*/
    private fun createNotification(): Notification {

        val title = notificationMessage

        val builder = NotificationCompat.Builder(
            this, channelId
        )
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setSmallIcon(cz.lastaapps.bakalari.core.R.drawable.icon)

        return builder.build()
    }
}