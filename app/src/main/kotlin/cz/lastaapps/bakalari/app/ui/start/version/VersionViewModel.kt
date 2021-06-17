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

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cz.lastaapps.bakalari.tools.startForegroundServiceCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VersionViewModel(private val app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext

    companion object {
        private val TAG get() = VersionViewModel::class.simpleName
    }

    val process = MutableLiveData(VersionProgressState.STATE_READY)

    private var job: Job? = null
    fun updateVersion() = synchronized(this) {

        if (job != null) {
            return@synchronized
        }
        process.postValue(VersionProgressState.STATE_READY)

        job = viewModelScope.launch(Dispatchers.Main) {
            process.postValue(VersionProgressState.STATE_RUNNING)

            val response = manageRespond()

            process.postValue(if (response) VersionProgressState.STATE_SUCCESS else VersionProgressState.STATE_FAILED)

            job = null
        }
    }

    private suspend fun manageRespond() = suspendCancellableCoroutine<Boolean> { continuation ->

        val filter = IntentFilter(VersionUpdateService.broadcastId)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                continuation.resume(intent.getBooleanExtra(VersionUpdateService.RESULT_KEY, false))
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        val intent = Intent(context, VersionUpdateService::class.java)
        context.startForegroundServiceCompat(intent)

        continuation.invokeOnCancellation {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }
}

enum class VersionProgressState {
    STATE_READY, STATE_RUNNING, STATE_SUCCESS, STATE_FAILED,
}