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

package cz.lastaapps.bakalari.app.ui.start.version

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.BaseForegroundService
import cz.lastaapps.bakalari.tools.normalizeID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*

class VersionUpdateService(
) : BaseForegroundService() {

    companion object {
        private val TAG = VersionUpdateService::class.simpleName

        val broadcastId = TAG
        const val RESULT_KEY = "RESULT"
    }

    override val notificationId = R.id.notification_id_version_update.normalizeID()
    override val notificationMessage = getString(R.string.version_update_service_text)

    @InternalCoroutinesApi
    override fun onCreate() {
        super.onCreate()

        val job = lifecycleScope.launch(Dispatchers.Default) {
            updateVersion()
        }
        job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
            sendResult(false)
        }
    }

    private suspend fun updateVersion() {
        VersionChecker.updated(this)
        try {
            val accounts = obtainAccounts()
            migrateDatabases(accounts.map { it.uuid })
            migrateNotificationChannels(accounts)

            sendResult(true)
        } catch (e: Exception) {
            e.printStackTrace()

            sendResult(false)
        }
    }

    private suspend fun obtainAccounts(): List<BakalariAccount> =
        AccountsDatabase.getDatabase(this).repository.getAll()

    private suspend fun migrateDatabases(list: List<UUID>) {
        var result = true
        for (uuid in list) {
            try {

                APIBase.getDatabase(this, uuid)
                APIBase.releaseDatabase(uuid)
                yield()
            } catch (e: Exception) {
                e.printStackTrace()
                result = false
            }
        }

        if (!result) throw IllegalStateException("Databases migration failed")
    }

    private suspend fun migrateNotificationChannels(list: List<BakalariAccount>) {
        //TODO migrate notification
    }

    private fun sendResult(result: Boolean) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(broadcastId).apply {
            putExtra(RESULT_KEY, result)
        })
    }
}