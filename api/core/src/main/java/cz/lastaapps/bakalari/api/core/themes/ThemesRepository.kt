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

package cz.lastaapps.bakalari.api.core.themes

import cz.lastaapps.bakalari.api.core.database.APIBase
import cz.lastaapps.bakalari.api.core.database.JSONStorageRepository
import cz.lastaapps.bakalari.api.core.database.RefreshingServerRepo
import cz.lastaapps.bakalari.api.core.themes.holders.ThemeList
import cz.lastaapps.bakalari.tools.TimeTools.toMidnight
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.ZonedDateTime

class ThemesRepository(database: APIBase, val subjectId: String) :
    RefreshingServerRepo<ThemeList>(TAG, database, "subjects/themes/$subjectId") {
    //: APIAssetRepo<ThemeList>(TAG, database, "themes.json", 1000) {

    companion object {
        private val TAG = ThemesRepository::class.java.simpleName
    }

    private val dao = database.themeDao()

    fun getThemes() = dao.getThemes(subjectId).distinctUntilChanged()
        .map { ThemeList(it) }.onDataUpdated {
            ThemeList(
                it.sorted()
            )
        }

    override fun lastUpdatedTables(): List<String> = listOf(APIBase.THEMES + subjectId)

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime =
        date.plusDays(1).toMidnight()

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveThemes(subjectId, json)

    override suspend fun parseData(json: JSONObject): ThemeList =
        ThemesParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: ThemeList): List<String> {
        dao.insertThemes(data)
        return lastUpdatedTables()
    }

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteSubject(subjectId)
    }
}