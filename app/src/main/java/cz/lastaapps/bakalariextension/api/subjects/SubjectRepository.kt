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

import cz.lastaapps.bakalariextension.api.database.APIBase
import cz.lastaapps.bakalariextension.api.database.JSONStorageRepository
import cz.lastaapps.bakalariextension.api.database.RefreshingServerRepo
import cz.lastaapps.bakalariextension.api.subjects.data.SubjectList
import cz.lastaapps.bakalariextension.api.subjects.data.SubjectTeacherLists
import cz.lastaapps.bakalariextension.api.subjects.data.TeacherList
import cz.lastaapps.bakalariextension.tools.sortList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.ZonedDateTime

class SubjectRepository(database: APIBase) :
    RefreshingServerRepo<SubjectTeacherLists>(TAG, database, "subjects") {
    //: APIAssetRepo<SubjectTeacherLists>(TAG, database, "subjects.json", 1000) {
    companion object {
        private val TAG = SubjectRepository::class.java.simpleName

        const val LOADING = 0
        const val FAILED = -1
        const val SUCCEEDED = 1
    }

    private val dao = database.subjectDao()

    fun getSubjects() = runBlocking(Dispatchers.IO) {
        dao.getSubjects().distinctUntilChanged().map { SubjectList(it) }
            .onDataUpdated { it.first.sortList() }
    }

    suspend fun getSubject(id: String) = dao.getSubject(id)

    fun getTeachers() = runBlocking(Dispatchers.IO) {
        dao.getTeachers().distinctUntilChanged().map { TeacherList(it) }
            .onDataUpdated { it.second.sortList() }
    }

    suspend fun getTeacher(id: String) = dao.getTeacher(id)

    suspend fun getSimpleTeacher(id: String) = dao.getSimpleTeacher(id)?.data

    suspend fun getTeachersSubjects(id: String) = SubjectList(dao.getTeachersSubjects(id))

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }

    override fun lastUpdatedTables(): List<String> = listOf(APIBase.SUBJECTS, APIBase.TEACHERS)

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime = date.plusDays(7)

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveSubjects(json)

    override suspend fun parseData(json: JSONObject): SubjectTeacherLists =
        SubjectParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: SubjectTeacherLists): List<String> {
        dao.insertSubjects(data.first)
        dao.insertTeachers(data.second)

        return lastUpdatedTables()
    }
}