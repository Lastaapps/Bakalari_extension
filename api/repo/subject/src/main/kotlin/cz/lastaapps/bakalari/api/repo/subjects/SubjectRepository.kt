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

package cz.lastaapps.bakalari.api.repo.subjects

import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.api.database.JSONStorageRepository
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.subjects.SubjectList
import cz.lastaapps.bakalari.api.entity.subjects.SubjectTeacherLists
import cz.lastaapps.bakalari.api.entity.subjects.TeacherList
import cz.lastaapps.bakalari.api.repo.core.RefreshingServerRepo
import cz.lastaapps.bakalari.tools.sortList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.ZonedDateTime

class SubjectRepository(database: APIBase) :
    RefreshingServerRepo<SubjectTeacherLists>(
        TAG, database, "subjects"
    ) {
    //: APIAssetRepo<SubjectTeacherLists>(TAG, database, "subjects.json", 1000) {
    companion object {
        private val TAG get() = SubjectRepository::class.java.simpleName

        const val LOADING = 0
        const val FAILED = -1
        const val SUCCEEDED = 1
    }

    private val dao = database.subjectDao()

    fun getSubjects() = runBlocking(Dispatchers.IO) {
        dao.getSubjects().distinctUntilChanged().map {
            SubjectList(it)
        }
            .onDataUpdated { it.first.sortList() }
    }

    suspend fun getSubject(id: String) = dao.getSubject(id)

    fun getTeachers() = runBlocking(Dispatchers.IO) {
        dao.getTeachers().distinctUntilChanged().map {
            TeacherList(it)
        }
            .onDataUpdated { it.second.sortList() }
    }

    suspend fun getTeacher(id: String) = dao.getTeacher(id)

    suspend fun getSimpleTeacher(id: String) = dao.getSimpleTeacher(id)?.data

    suspend fun getTeachersSubjects(id: String) =
        cz.lastaapps.bakalari.api.entity.subjects.SubjectList(dao.getTeachersSubjects(id))

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }

    override fun lastUpdatedTables(): List<String> =
        listOf(APIBaseKeys.SUBJECTS, APIBaseKeys.TEACHERS)

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime = date.plusDays(7)

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveSubjects(json)

    override suspend fun parseData(json: JSONObject): SubjectTeacherLists =
        cz.lastaapps.bakalari.api.entity.subjects.SubjectParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: SubjectTeacherLists): List<String> {
        dao.insertSubjects(data.first)
        dao.insertTeachers(data.second)

        return lastUpdatedTables()
    }
}