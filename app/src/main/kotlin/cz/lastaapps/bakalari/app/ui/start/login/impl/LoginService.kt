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
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.platform.BaseForegroundService
import cz.lastaapps.bakalari.tools.normalizeID
import cz.lastaapps.bakalari.tools.startForegroundServiceCompat
import kotlinx.coroutines.launch
import java.util.*

class LoginService : BaseForegroundService() {

    companion object {

        private val TAG = LoginService::class.simpleName
        private const val ACCOUNT_KEY = "ACCOUNT_KEY"

        private const val BROADCAST_ID = "LOGIN"
        const val KEY_UUID = "UUID"
        const val KEY_STATE = "STATE"

        fun startService(context: Context, account: BakalariAccount) {
            context.startForegroundServiceCompat(Intent(context, LoginService::class.java).apply {
                putExtra(ACCOUNT_KEY, account)
            })
        }

        //TODO implement per uuid
        fun stopService(context: Context, uuid: UUID) =
            context.stopService(Intent(context, LoginService::class.java))


        fun broadcastId(uuid: UUID) = "$BROADCAST_ID $uuid"
    }

    private lateinit var account: BakalariAccount
    override val notificationId = R.id.notification_id_login_service.normalizeID()
    override val notificationMessage by lazy { getString(R.string.login_services_in_channel_name) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        account = intent!!.getParcelableExtra(ACCOUNT_KEY)!!
        lifecycleScope.launch {
            val impl = LoginImpl(this@LoginService, account)
            val state = impl.doLogin()

            val dataIntent = Intent(broadcastId(account.uuid)).apply {
                putExtra(KEY_UUID, account.uuid)
                putExtra(KEY_STATE, state)
            }
            LocalBroadcastManager.getInstance(this@LoginService).sendBroadcast(dataIntent)

            stopForeground(true)
        }

        return START_NOT_STICKY
    }
}