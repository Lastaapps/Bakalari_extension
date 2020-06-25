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

package cz.lastaapps.bakalariextension.api.absence

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceRoot
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AbsenceLoader {
    companion object {
        private val TAG = AbsenceLoader::class.java.simpleName

        /**Tries to load marks
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        suspend fun load(forceReload: Boolean = false): AbsenceRoot? {

            return withContext(Dispatchers.Default) {
                var toReturn: AbsenceRoot? = null

                if (forceReload || AbsenceStorage.lastUpdated() == null) {
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

        /**Tries to load from server and save data to local storage
         * @return downloaded data object or null, if download failed*/
        suspend fun loadFromServer(): AbsenceRoot? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Loading from server")

                    //downloads marks
                    val json = withContext(Dispatchers.IO) { ConnMgr.serverGet("absence/student") }
                        ?: return@withContext null

                    //parses json
                    val week = AbsenceParser.parseJson(json)

                    //saves json
                    AbsenceStorage.save(json)

                    return@withContext week

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Loads data from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(): AbsenceRoot? {
            return withContext(Dispatchers.Default) {

                Log.i(TAG, "Loading from storage")

                return@withContext try {

                    //load json from storage
                    val json = withContext(Dispatchers.IO) { AbsenceStorage.load() }
                        ?: return@withContext null

                    AbsenceParser.parseJson(json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        /**@return if marks are "old" enough to be reloaded*/
        fun shouldReload(): Boolean {
            val lastUpdated = AbsenceStorage.lastUpdated()
            if (lastUpdated != null) {
                if (lastUpdated.isAfter(TimeTools.now.minusHours(24))) {
                    return false
                }
            }

            return true
        }
    }
}