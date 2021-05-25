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

package cz.lastaapps.bakalari.api.core.web

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import cz.lastaapps.bakalari.api.core.ConnMgr
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import kotlinx.coroutines.yield


/**Obtains login required to onetime browser login and url to put into browser*/
object LoginToken {

    private val TAG = LoginToken::class.java.simpleName

    suspend fun loadFromServer(appContext: Context, account: BakalariAccount): String? {

        Log.i(TAG, "Loading from server")

        return ConnMgr.serverGetString(appContext, account, "logintoken")
            ?.replace("\"", "")
    }

    @WorkerThread
    suspend fun loginUrl(appContext: Context, account: BakalariAccount): String? {
        val webRoot = WebModulesLoader.loadFromServer(appContext, account) ?: return null

        yield()

        val loginToken = loadFromServer(appContext, account) ?: return null

        return account.url + "/api/3/login/" + loginToken + "?returnUrl=" + webRoot.dashboard.url
    }
}