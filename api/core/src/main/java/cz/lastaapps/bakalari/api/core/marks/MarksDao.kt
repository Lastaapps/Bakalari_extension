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

package cz.lastaapps.bakalari.api.core.marks

import androidx.room.*
import cz.lastaapps.bakalari.api.core.database.APIBase
import cz.lastaapps.bakalari.api.core.marks.holders.Mark
import cz.lastaapps.bakalari.api.core.marks.holders.MarksPair
import cz.lastaapps.bakalari.api.core.marks.holders.MarksSubject
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

@Dao
abstract class MarksDao {

    @Transaction
    @Query("SELECT * FROM ${APIBase.MARK_SUBJECT} ORDER BY name COLLATE LOCALIZED")
    abstract fun getAllPairs(): Flow<List<MarksPair>>

    @Transaction
    @Query("SELECT * FROM ${APIBase.MARK_SUBJECT} WHERE id=:subjectId LIMIT 1")
    abstract fun getPair(subjectId: String): Flow<MarksPair?>


    @Query("SELECT * FROM (SELECT * FROM ${APIBase.MARKS} WHERE editDate > :date OR date > :date ORDER BY date DESC) UNION SELECT * FROM ${APIBase.MARKS} WHERE editDate <= :date AND date <= :date ORDER BY date DESC")
    abstract fun getAllMarks(date: ZonedDateTime): Flow<List<Mark>>

    @Query("SELECT * FROM (SELECT * FROM ${APIBase.MARKS} WHERE (editDate > :date OR date > :date) AND subjectId=:subjectId ORDER BY date DESC) UNION SELECT * FROM ${APIBase.MARKS} WHERE editDate <= :date AND date <= :date AND subjectId=:subjectId ORDER BY date DESC")
    abstract fun getMarks(subjectId: String, date: ZonedDateTime): Flow<List<Mark>>

    @Query("SELECT * FROM ${APIBase.MARKS} WHERE editDate > :date OR date > :date ORDER BY date DESC")
    abstract fun getNewMarks(date: ZonedDateTime): Flow<List<Mark>>

    @Query("SELECT * FROM ${APIBase.MARKS} WHERE id=:markId LIMIT 1")
    abstract fun getMark(markId: String): Mark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMarks(list: List<Mark>)

    @Delete
    abstract suspend fun deleteMarks(list: List<Mark>)

    @Query("DELETE FROM ${APIBase.MARKS}")
    abstract suspend fun deleteAllMarks()


    @Query("SELECT * FROM ${APIBase.MARK_SUBJECT} ORDER BY name COLLATE LOCALIZED")
    abstract fun getSubjects(): Flow<List<MarksSubject>>

    @Query("SELECT * FROM ${APIBase.MARK_SUBJECT} WHERE id=:subjectId LIMIT 1")
    abstract fun getSubject(subjectId: String): MarksSubject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSubjects(list: List<MarksSubject>)

    @Delete
    abstract suspend fun deleteSubjects(list: List<Mark>)

    @Query("DELETE FROM ${APIBase.MARK_SUBJECT}")
    abstract suspend fun deleteAllSubjects()

    @Transaction
    open suspend fun deleteAll() {
        deleteAllMarks()
        deleteAllSubjects()
    }
}