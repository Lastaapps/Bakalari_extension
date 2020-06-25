/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalariextension.api.user

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.user.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserLoader {
    companion object {
        private val TAG = User::class.java.simpleName

        /**Tries to load subjects
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        suspend fun load(forceReload: Boolean = false): User? {

            return withContext(Dispatchers.Default) {
                var toReturn: User? = null

                if (forceReload) {
                    toReturn = loadFromServer()
                } else {
                    if (!shouldReload())
                        toReturn = loadFromStorage()

                    if (toReturn == null) {
                        toReturn = loadFromServer()
                    }
                }

                return@withContext toReturn
            }
        }

        /**Tries load subjects from server and save him to local storage
         * @return downloaded AllSubjects object or null, if download failed*/
        suspend fun loadFromServer(): User? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Loading subjects from server")

                    //downloads subjects
                    val json = withContext(Dispatchers.IO) { ConnMgr.serverGet("user") }
                        ?: return@withContext null

                    //parses json
                    val data = UserParser.parseJson(json)

                    //saves json
                    UserStorage.save(json)

                    return@withContext data

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Loads subject from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(): User? {
            return withContext(Dispatchers.Default) {

                Log.i(TAG, "Loading subjects from storage")

                return@withContext try {

                    //load json from storage
                    val json = withContext(Dispatchers.IO) { UserStorage.load() }
                        ?: return@withContext null

                    UserParser.parseJson(json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        /**@return if subjects are loaded*/
        fun shouldReload(): Boolean {
            UserStorage.lastUpdated() ?: return true
            return false
        }
    }
}