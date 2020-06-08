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

package cz.lastaapps.bakalariextension.api.homework

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.ZonedDateTime

class HomeworkLoader {
    companion object {
        private val TAG = HomeworkLoader::class.java.simpleName

        /**Tries to load homework
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        suspend fun loadHomework(
            from: ZonedDateTime = TimeTools.firstSeptember,
            forceReload: Boolean = false
        ): HomeworkList? {
            return withContext(Dispatchers.Default) {

                var toReturn: HomeworkList? = null

                if (forceReload || HomeworkStorage.lastUpdated() == null) {
                    toReturn = loadFromServer(from)
                } else {
                    if (!shouldReload())
                        toReturn = loadFromStorage()

                    if (toReturn == null) {
                        toReturn = loadFromServer(from)
                    }
                }

                return@withContext toReturn
            }
        }

        /**Tries load homework from server and save him to local storage
         * @return downloaded AllSubjects object or null, if download failed*/
        suspend fun loadFromServer(from: ZonedDateTime = TimeTools.firstSeptember): HomeworkList? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Loading homework from server")

                    //downloads homework
                    val dataMap = mapOf(Pair("from", TimeTools.format(from, TimeTools.DATE_FORMAT)))
                    val json =
                        withContext(Dispatchers.IO) { ConnMgr.serverGet("homeworks", dataMap) }
                            ?: return@withContext null

                    //parses json
                    val week = HomeworkParser.parseJson(json)

                    //saves json
                    save(json)

                    return@withContext week

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Loads homework from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(): HomeworkList? {
            return withContext(Dispatchers.Default) {

                Log.i(TAG, "Loading homework from storage")

                return@withContext try {

                    //load json from storage
                    val json = withContext(Dispatchers.IO) { HomeworkStorage.load() }
                        ?: return@withContext null

                    HomeworkParser.parseJson(json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        /**Updates homework with new data
         * useful when only last week was loaded, so old data are updated, not replaced*/
        private fun save(json: JSONObject) {
            val saved = HomeworkStorage.load()
            if (saved == null) {
                //no new homework
                HomeworkStorage.save(json)
            } else {
                val new = HomeworkParser.parseJson(json)
                //false when no new data downloaded
                if (new.isNotEmpty()) {
                    val old = HomeworkParser.parseJson(saved)

                    val combined = HashSet<Homework>().apply {
                        //ads old data to new, shorter package
                        addAll(new)
                        addAll(old)
                    }

                    HomeworkStorage.save(HomeworkParser.encodeJson(combined.toList()))
                }
            }
        }

        /**@return if homework are "old" enough to be reloaded*/
        fun shouldReload(): Boolean {
            val lastUpdated = HomeworkStorage.lastUpdated()
            if (lastUpdated != null) {
                if (lastUpdated.isAfter(TimeTools.now.minusDays(1))) {
                    return false
                }
            }

            return true
        }
    }
}