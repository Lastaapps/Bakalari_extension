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

package cz.lastaapps.bakalari.api.repo.timetable

import cz.lastaapps.bakalari.api.database.JSONStorageRepository
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.timetable.Week
import cz.lastaapps.bakalari.api.repo.core.RefreshingRepoJSON
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toMonday
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimetableRepository(
    database: cz.lastaapps.bakalari.api.database.APIBase,
    val monday: LocalDate
) :
    RefreshingRepoJSON<Week>(
        TAG, database
    ) {

    companion object {
        private val TAG get() = TimetableRepository::class.java.simpleName
    }

    private val dao = database.timetableDao()

    fun getWeek() = dao.getWeek(monday).onDataUpdated()

    fun getWeekForDate(date: LocalDate) = dao.getWeekForDay(date).distinctUntilChanged()

    override fun lastUpdatedTables(): List<String> = listOf(
        monday.format(DateTimeFormatter.ISO_LOCAL_DATE),
        APIBaseKeys.TIMETABLE_DAY,
        APIBaseKeys.TIMETABLE_LESSON,
        APIBaseKeys.TIMETABLE_CHANGE,
        APIBaseKeys.TIMETABLE_HOUR,
        APIBaseKeys.TIMETABLE_LESSON_CYCLE,
        APIBaseKeys.TIMETABLE_LESSON_GROUP,
        APIBaseKeys.TIMETABLE_LESSON_HOMEWORK,
    )

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime {
        return if (date.toMonday() >= ZonedDateTime.now().toMonday()) {
            date.plusDays(1)
        } else {
            //doesn't auto update old timetables
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), TimeTools.UTC)
        }
    }

    override suspend fun loadFromServer(): JSONObject? {
        val loadFromAssets = false
        return if (!loadFromAssets) {
            if (monday == TimeTools.PERMANENT) {
                loadFromServer("timetable/permanent")
            } else {
                loadFromServer("timetable/actual?date=${monday.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
            }
        } else {
            if (monday == TimeTools.PERMANENT) {
                loadFromAssets("timetable_permanent.json")
            } else {
                loadFromAssets("timetable.json")
            }
        }
    }

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveTimetable(monday, json)

    override suspend fun parseData(json: JSONObject): Week =
        cz.lastaapps.bakalari.api.entity.timetable.TimetableParser.parseJson(monday, json)

    override suspend fun insertIntoDatabase(data: Week): List<String> {
        dao.replaceWeek(data)
        return lastUpdatedTables()
    }

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.delete(monday)
    }
}