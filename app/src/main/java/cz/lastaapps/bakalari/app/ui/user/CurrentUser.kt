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

package cz.lastaapps.bakalari.app.ui.user

import android.content.Context
import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.App
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import java.util.*

object CurrentUser {

    val accountUUID = ConflatedBroadcastChannel<UUID?>(null)

    suspend fun getAccount(context: Context): BakalariAccount? {
        accountUUID.value?.let {
            return AccountsDatabase.getDatabase(context).repository.getByUUID(it)
        }
        return null
    }

    suspend inline fun requireAccount(context: Context) = getAccount(context)!!

    val database
        get() = if (accountUUID.value != null) {
            APIBase.getDatabaseBlocking(App.context, accountUUID.value!!)
        } else null

    fun requireDatabase() = database!!

    suspend fun releaseDatabase() {
        accountUUID.value?.let { APIBase.releaseDatabase(it) }
    }

    suspend fun deleteDatabase(context: Context) {
        accountUUID.value?.let { APIBase.deleteDatabase(context, it) }
    }
}