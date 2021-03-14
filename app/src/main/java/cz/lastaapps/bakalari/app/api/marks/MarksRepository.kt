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

package cz.lastaapps.bakalari.app.api.marks

import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.app.api.database.JSONStorageRepository
import cz.lastaapps.bakalari.app.api.database.RefreshingServerRepo
import cz.lastaapps.bakalari.app.api.marks.data.*
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.ZonedDateTime

class MarksRepository(database: APIBase) :
    RefreshingServerRepo<MarksSubjectMarksLists>(TAG, database, "marks") {
//    : RefreshingAssetRepo<MarksSubjectMarksLists>(TAG, database, "marks.json", 1000) {

    companion object {
        private val TAG = MarksRepository::class.java.simpleName
    }

    fun getAllPairs() = dao.getAllPairs().distinctUntilChanged().map { MarksPairList(it) }

    fun getPair(subjectId: String) = dao.getPair(subjectId)

    fun getAllMarks(date: ZonedDateTime = markNewDate()) =
        dao.getAllMarks(date).distinctUntilChanged().map { MarksList(it) }

    fun getMarks(subjectId: String, date: ZonedDateTime = markNewDate()) =
        dao.getMarks(subjectId, date).distinctUntilChanged().map { MarksList(it) }

    fun getNewMarks(date: ZonedDateTime = markNewDate()) =
        dao.getNewMarks(date).distinctUntilChanged().map { MarksList(it) }

    fun getMark(markId: String): Mark? = dao.getMark(markId)


    fun getSubjects() = dao.getSubjects().distinctUntilChanged().map { MarksSubjectList(it) }

    fun getSubject(subjectId: String): MarksSubject? = dao.getSubject(subjectId)

    /**@return date from which marks are considered as new*/
    private fun markNewDate(): ZonedDateTime =
        ZonedDateTime.now().minusDays(MySettings.withAppContext().getNewMarkDuration().toLong())


    private val dao = database.marksDao()

    override fun lastUpdatedTables(): List<String> = listOf(APIBase.MARKS, APIBase.SUBJECTS)

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime = date.plusHours(6)

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveMarks(json)

    override suspend fun parseData(json: JSONObject): MarksSubjectMarksLists =
        MarksParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: MarksSubjectMarksLists): List<String> {
        dao.insertSubjects(data.first)
        dao.insertMarks(data.second)
        return lastUpdatedTables()
    }

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }
}