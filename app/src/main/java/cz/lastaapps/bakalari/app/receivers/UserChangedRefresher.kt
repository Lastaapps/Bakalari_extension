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
import cz.lastaapps.bakalari.api.repo.user.UserChangeObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**deletes the old data when user object changes*/
class UserChangedRefresher : BroadcastReceiver() {

    companion object {
        private val TAG = UserChangedRefresher::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == UserChangeObserver.USER_CHANGED) {

            Log.i(TAG, "Refreshing data based on the user object change")

            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    //TODO user refresh
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                pending.finish()
            }
        }
    }
}