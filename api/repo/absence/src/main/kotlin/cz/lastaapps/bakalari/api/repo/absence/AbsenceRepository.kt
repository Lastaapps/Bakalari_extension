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

package cz.lastaapps.bakalari.api.repo.absence

import cz.lastaapps.bakalari.api.database.JSONStorageRepository
import cz.lastaapps.bakalari.api.entity.absence.AbsenceRoot
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.repo.core.RefreshingServerRepo
import cz.lastaapps.bakalari.tools.TimeTools.toMidnight
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.ZonedDateTime

class AbsenceRepository(database: cz.lastaapps.bakalari.api.database.APIBase) :
    RefreshingServerRepo<AbsenceRoot>(
        TAG, database, "absence/student"
    ) {
    //: APIAssetRepo<AbsenceRoot>(TAG, database, "absence_student.json", 1000) {

    companion object {
        private val TAG = AbsenceRepository::class.java.simpleName
    }

    private val dao = database.absenceDao()


    fun getThreshold() =
        dao.getThreshold().map { it?.threshold }.onDataUpdated { it.percentageThreshold }

    fun getDays() = dao.getDays().map { DataIdList(it) }.onDataUpdated { it.days }

    suspend fun getDay(date: ZonedDateTime) = dao.getDay(date)

    fun getSubjects() = dao.getSubjects().map { DataIdList(it) }.onDataUpdated { it.subjects }

    suspend fun getSubject(subjectName: String) = dao.getSubject(subjectName)


    override fun lastUpdatedTables(): List<String> =
        listOf(APIBaseKeys.ABSENCE_THRESHOLD, APIBaseKeys.ABSENCE_DAY, APIBaseKeys.ABSENCE_SUBJECT)

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime =
        date.toMidnight().plusDays(1)

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveAbsence(json)

    override suspend fun parseData(json: JSONObject): AbsenceRoot =
        cz.lastaapps.bakalari.api.entity.absence.AbsenceParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: AbsenceRoot): List<String> {
        dao.replaceWith(data)
        return lastUpdatedTables()
    }

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }
}