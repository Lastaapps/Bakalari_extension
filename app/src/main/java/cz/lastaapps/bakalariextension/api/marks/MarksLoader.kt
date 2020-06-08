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

package cz.lastaapps.bakalariextension.api.marks

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarksLoader {
    companion object {
        private val TAG = MarksLoader::class.java.simpleName

        /**Tries to load marks
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        suspend fun loadMarks(forceReload: Boolean = false): MarksAllSubjects? {

            return withContext(Dispatchers.Default) {
                var toReturn: MarksAllSubjects? = null

                if (forceReload || MarksStorage.lastUpdated() == null) {
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

        /**Tries load marks from server and save him to local storage
         * @return downloaded AllSubjects object or null, if download failed*/
        suspend fun loadFromServer(): MarksAllSubjects? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Loading marks from server")

                    //downloads marks
                    val json = withContext(Dispatchers.IO) { ConnMgr.serverGet("marks") }
                        ?: return@withContext null

                    //parses json
                    val week = MarksParser.parseJson(json)

                    //saves json
                    MarksStorage.save(json)

                    return@withContext week

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Loads marks from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(): MarksAllSubjects? {
            return withContext(Dispatchers.Default) {

                Log.i(TAG, "Loading marks from storage")

                return@withContext try {

                    //load json from storage
                    val json = withContext(Dispatchers.IO) { MarksStorage.load() }
                        ?: return@withContext null

                    MarksParser.parseJson(json)

                    //for testing empty subject, subject with mixed marks and new marks
                    /*.apply {

                                val sm = SubjectMarks(
                                    DataIdList(),
                                    Subject("test1", "Sbj 1", "Test 1"),
                                    "", "", "", "", false, true
                                )
                                subjects.add(sm)
                                val sm2 = SubjectMarks(
                                    DataIdList(
                                        listOf(
                                            Mark.default.apply {
                                                editDate = TimeTools.format(
                                                    TimeTools.now,
                                                    TimeTools.COMPLETE_FORMAT
                                                )
                                            },
                                            Mark.default
                                        )
                                    ),
                                    Subject("test2", "Sbj 2", "Test 2"),
                                    "", "", "", "", false, true
                                )
                                subjects.add(sm2)
                            }*/

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        /**@return if marks are "old" enough to be reloaded*/
        fun shouldReload(): Boolean {
            val lastUpdated = MarksStorage.lastUpdated()
            if (lastUpdated != null) {
                if (lastUpdated.isAfter(TimeTools.now.minusDays(1))) {
                    return false
                }
            }

            return true
        }
    }
}