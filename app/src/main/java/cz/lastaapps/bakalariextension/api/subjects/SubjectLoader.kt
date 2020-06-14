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

package cz.lastaapps.bakalariextension.api.subjects

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.subjects.data.Subject
import cz.lastaapps.bakalariextension.api.subjects.data.Teacher
import cz.lastaapps.bakalariextension.api.subjects.data.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias ThemeList = DataIdList<Theme>
typealias SubjectList = DataIdList<Subject>
typealias TeacherList = DataIdList<Teacher>

/**Loads subjects and themes (server only)*/
class SubjectLoader {
    companion object {
        private val TAG = SubjectLoader::class.java.simpleName

        /**Tries to load subjects
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        suspend fun load(forceReload: Boolean = false): SubjectList? {

            return withContext(Dispatchers.Default) {
                var toReturn: SubjectList? = null

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
        suspend fun loadFromServer(): SubjectList? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Loading subjects from server")

                    //downloads subjects
                    val json = withContext(Dispatchers.IO) { ConnMgr.serverGet("subjects") }
                        ?: return@withContext null

                    //parses json
                    val data = SubjectThemeParser.parseSubjectJson(json)

                    //saves json
                    SubjectStorage.save(json)

                    return@withContext data

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Loads subject from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(): SubjectList? {
            return withContext(Dispatchers.Default) {

                Log.i(TAG, "Loading subjects from storage")

                return@withContext try {

                    //load json from storage
                    val json = withContext(Dispatchers.IO) { SubjectStorage.load() }
                        ?: return@withContext null

                    SubjectThemeParser.parseSubjectJson(json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        /**Tries load themes from server
         * @return downloaded themes list or null when download failed*/
        suspend fun loadThemesFromServer(id: String): ThemeList? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Loading themes from server")

                    //downloads subjects
                    val json =
                        withContext(Dispatchers.IO) { ConnMgr.serverGet("subjects/themes/$id") }
                            ?: return@withContext null

                    return@withContext SubjectThemeParser.parseThemeJson(json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**@return if subjects are loaded*/
        private fun shouldReload(): Boolean {
            SubjectStorage.lastUpdated() ?: return false
            return true
        }
    }
}