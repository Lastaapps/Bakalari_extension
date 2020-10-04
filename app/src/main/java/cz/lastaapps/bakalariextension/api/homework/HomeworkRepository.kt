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

import cz.lastaapps.bakalariextension.api.database.APIBase
import cz.lastaapps.bakalariextension.api.database.JSONStorageRepository
import cz.lastaapps.bakalariextension.api.database.RefreshingRepoJSON
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toMidnight
import cz.lastaapps.bakalariextension.tools.sortList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.ZonedDateTime

class HomeworkRepository(database: APIBase) : RefreshingRepoJSON<HomeworkList>(TAG, database) {

    companion object {
        private val TAG = HomeworkRepository::class.java.simpleName
    }

    private val dao = database.homeworkDao()

    fun getAllHomeworkList() =
        dao.getHomeworkList().distinctUntilChanged()
            .map { list -> HomeworkList(list.map { it.toHomework() }) }
            .onDataUpdated { it.sortList() }

    fun getCurrentHomeworkList(date: ZonedDateTime = getBoundaryDay()) =
        dao.getCurrentHomeworkList(date).distinctUntilChanged()
            .map { list -> HomeworkList(list.map { it.toHomework() }) }

    fun getOldHomeworkList(date: ZonedDateTime = getBoundaryDay()) =
        dao.getOldHomeworkList(date).distinctUntilChanged()
            .map { list -> HomeworkList(list.map { it.toHomework() }) }

    suspend fun getHomeworkListForSubject(id: String) =
        HomeworkList(dao.getHomeworkListForSubject(id).map { it.toHomework() })

    suspend fun getHomework(id: String): Homework? = dao.getHomework(id)?.toHomework()

    suspend fun isCurrent(id: String, date: ZonedDateTime = getBoundaryDay()): Boolean =
        dao.getCurrentIds(date).contains(id)


    private fun getBoundaryDay(): ZonedDateTime = ZonedDateTime.now()

    override fun lastUpdatedTables(): List<String> =
        listOf(APIBase.HOMEWORK, APIBase.HOMEWORK_ATTACHMENTS)

    override suspend fun loadFromServer(): JSONObject? {
        //val args = mapOf(Pair("from", TimeTools.format(from, TimeTools.DATE_FORMAT)))
        return loadFromServer("homeworks" /*args*/)

        //delay(1000)
        //return loadFromAssets("homework.json")
    }

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveHomework(json)

    override suspend fun parseData(json: JSONObject): HomeworkList =
        HomeworkParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: HomeworkList): List<String> {
        dao.replaceData(data)
        return lastUpdatedTables()
    }

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime =
        date.toMidnight().plusDays(1)

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }
}