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

class Marks {
    companion object {
        private val TAG = Marks::class.java.simpleName

        /**Tries to load marks
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        fun loadMarks(forceReload: Boolean = false): MarksAllSubjects? {

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

            return toReturn
        }

        /**Tries load marks from server and save him to local storage
         * @return downloaded AllSubjects object or null, if download failed*/
        fun loadFromServer(): MarksAllSubjects? {
            try {
                Log.i(TAG, "Loading marks from server")

                //downloads normal or permanent timetable
                val json = ConnMgr.serverGet("marks") ?: return null

                //parses json
                val week = JSONParser.parseJson(json)

                //saves json
                MarksStorage.save(json)

                return week

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**Loads marks from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        fun loadFromStorage(): MarksAllSubjects? {

            Log.i(TAG, "Loading marks from storage")

            return try {

                //load json from storage
                val json = MarksStorage.load() ?: return null
                JSONParser.parseJson(json)

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