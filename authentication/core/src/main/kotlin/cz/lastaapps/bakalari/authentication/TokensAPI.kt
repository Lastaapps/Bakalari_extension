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

package cz.lastaapps.bakalari.authentication

import android.app.backup.BackupManager
import android.content.Context
import android.util.Log
import cz.lastaapps.bakalari.authentication.data.LoginInfo
import cz.lastaapps.bakalari.authentication.data.Tokens
import cz.lastaapps.bakalari.authentication.data.getRawUrl
import cz.lastaapps.bakalari.authentication.data.toTokens
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class TokensAPI(private val context: Context) {

    companion object {
        private val TAG get() = TokensAPI::class.simpleName

        const val SUCCESS = 0
        const val OLD_TOKENS = 1
        const val SERVER_ERROR = -1
        const val INTERNET_ERROR = -2
    }

    private val database = AccountsDatabase.getDatabase(context)
    private val mutexMap = HashMap<UUID, Mutex>()


    suspend fun getNewAccessToken(loginInfo: LoginInfo): Pair<Int, Tokens?> =
        getMutex(loginInfo.uuid).withLock {
            withContext(Dispatchers.IO) {


                Log.i(TAG, "Getting new access token")

                if (loginInfo.password == null) throw IllegalArgumentException("Password cannot be null when obtaining new tokens")

                var newTokens: Tokens? = null

                val dataUpdated = { json: JSONObject ->

                    //notifies that backup should be made
                    BackupManager.dataChanged(context.packageName)

                    newTokens = json.toTokens(loginInfo.uuid, loginInfo.url)
                }
                val networking = Networking(context, loginInfo.url)

                val code =
                    when (networking.obtainTokens(
                        loginInfo.userName,
                        loginInfo.password,
                        dataUpdated
                    )) {
                        Networking.LOGIN_OK -> SUCCESS
                        Networking.LOGIN_WRONG -> SERVER_ERROR
                        else -> INTERNET_ERROR
                    }
                Log.i(TAG, "New token response code $code")

                Pair(code, newTokens)
            }
        }

    suspend fun getRefreshedToken(
        uuid: UUID, force: Boolean = false
    ): Pair<Int, Tokens?> = getMutex(uuid).withLock {
        withContext(Dispatchers.IO) {


            Log.i(TAG, "Refreshing tokens")

            val account = database.repository.getByUUID(uuid)!!
            val tokens = account.toTokens()
            var newTokens: Tokens? = null

            val dataUpdated = { json: JSONObject ->

                //notifies that backup should be made
                BackupManager.dataChanged(context.packageName)

                newTokens = json.toTokens(tokens.uuid, tokens.url)
            }
            val networking = Networking(context, tokens.url.getRawUrl())

            return@withContext if (tokens.isExpired() || force) {
                val code = when (networking.refreshAccessToken(tokens.refreshToken, dataUpdated)) {
                    Networking.LOGIN_OK -> {
                        database.repository.updateToken(newTokens!!)
                        SUCCESS
                    }
                    Networking.LOGIN_WRONG -> {
                        //TODO move (maybe)
                        LoginModuleConfig.onInvalidRefreshToken(context)

                        val loginInfo = account.toLoginInfo()
                        if (loginInfo.password != null) {

                            Log.i(TAG, "Trying to obtain new tokens pair")
                            val result = getNewAccessToken(loginInfo)

                            when (result.first) {
                                SUCCESS -> return@withContext Pair(SUCCESS, result.second!!)
                                INTERNET_ERROR -> return@withContext Pair(INTERNET_ERROR, newTokens)
                            }
                        }

                        SERVER_ERROR
                    }
                    else -> INTERNET_ERROR
                }
                Log.i(TAG, "Refresh response code $code")
                Pair(code, newTokens)
            } else {
                Log.i(TAG, "Tokens weren't updated")
                Pair(OLD_TOKENS, tokens)
            }
        }
    }

    private fun getMutex(uuid: UUID): Mutex = synchronized(this) {
        return if (!mutexMap.containsKey(uuid)) {
            Mutex().also {
                mutexMap[uuid] = it
            }
        } else {
            mutexMap[uuid]!!
        }
    }
}