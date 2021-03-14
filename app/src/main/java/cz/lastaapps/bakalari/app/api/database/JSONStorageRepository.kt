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

package cz.lastaapps.bakalari.app.api.database

import androidx.room.*
import cz.lastaapps.bakalari.tools.TimeTools
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**Stores raw JSONs downloaded from server*/
class JSONStorageRepository(val database: APIBase) {

    companion object {
        private const val ABSENCE = "absence"
        private const val EVENTS = "events"
        private const val HOMEWORK = "homework"
        private const val MARKS = "marks"
        private const val SUBJECT = "subject"
        private const val THEMES = "themes"
        private const val TIMETABLE = "timetable"
        private const val USER = "user"
        private const val WEB = "web"

        /**if data saving is enabled*/
        private const val ENABLED = true // <-------------------------------------------------------
    }

    private val dao = database.jsonStorageDao()

    fun saveAbsence(json: JSONObject?) = insert(ABSENCE, json)
    fun getAbsence() = get(ABSENCE)

    fun saveEvents(type: String, json: JSONObject?) = insert(EVENTS + type, json)
    fun getEvents(type: String) = get(EVENTS + type)

    fun saveHomework(json: JSONObject?) = insert(HOMEWORK, json)
    fun getHomework() = get(HOMEWORK)

    fun saveMarks(json: JSONObject?) = insert(MARKS, json)
    fun getMarks() = get(MARKS)

    fun saveSubjects(json: JSONObject?) = insert(SUBJECT, json)
    fun getSubjects() = get(SUBJECT)

    fun saveThemes(subjectId: String, json: JSONObject?) = insert(THEMES + subjectId, json)
    fun getThemes(subjectId: String) = get(THEMES + subjectId)

    fun saveTimetable(date: LocalDate, json: JSONObject?) =
        insert(TIMETABLE + date.format(DateTimeFormatter.BASIC_ISO_DATE), json)

    fun getTimetable(date: ZonedDateTime) =
        get(TIMETABLE + TimeTools.format(date, TimeTools.COMPLETE_FORMAT))

    fun getAllTimetables(): List<JSONObject> = getAllIds(TIMETABLE)

    fun saveUser(json: JSONObject?) = insert(USER, json)
    fun getUser() = get(USER)

    fun saveWeb(json: JSONObject?) = insert(WEB, json)
    fun getWeb() = get(WEB)


    private fun get(key: String): JSONObject? = dao.getPair(key)?.data

    private fun getAllIds(key: String): List<JSONObject> = dao.getAllPairsData("$key%")

    private fun insert(key: String, json: JSONObject?) {
        if (ENABLED)
            if (json != null) {
                dao.insert(listOf(JSONPair(key, json)))
            } else {
                dao.delete(listOf(JSONPair(key, null)))
            }
    }

    fun deleteAll() = dao.deleteAll()

    @Dao
    abstract class JSONStorageDao {

        @Query("SELECT * FROM ${APIBase.JSON_STORAGE} WHERE id=:key")
        abstract fun getPair(key: String): JSONPair?

        @Query("SELECT data FROM ${APIBase.JSON_STORAGE} WHERE id LIKE :key")
        abstract fun getAllPairsData(key: String): List<JSONObject>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract fun insert(list: List<JSONPair>)

        @Delete
        abstract fun delete(list: List<JSONPair>)

        @Query("DELETE FROM ${APIBase.JSON_STORAGE}")
        abstract fun deleteAll()
    }

    @Entity(tableName = APIBase.JSON_STORAGE)
    data class JSONPair(@PrimaryKey val id: String, val data: JSONObject?)
}