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

package cz.lastaapps.bakalari.api.dao.absence

import androidx.room.*
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

@Dao
abstract class AbsenceDao {

    @Transaction
    open suspend fun replaceWith(root: cz.lastaapps.bakalari.api.entity.absence.AbsenceRoot) =
        root.apply {
            replaceThreshold(
                ThresholdHolder(
                    percentageThreshold
                )
            )
            replaceDays(days)
            replaceSubjects(subjects)
        }

    @Query("SELECT * FROM ${APIBaseKeys.ABSENCE_THRESHOLD}")
    abstract fun getThreshold(): Flow<ThresholdHolder?>

    @Insert
    protected abstract suspend fun insertThreshold(threshold: ThresholdHolder)

    @Query("DELETE FROM ${APIBaseKeys.ABSENCE_THRESHOLD}")
    protected abstract suspend fun deleteThreshold()

    @Transaction
    open suspend fun replaceThreshold(threshold: ThresholdHolder) {
        deleteThreshold()
        insertThreshold(threshold)
    }


    @Query("SELECT * FROM ${APIBaseKeys.ABSENCE_DAY}")
    abstract fun getDays(): Flow<List<cz.lastaapps.bakalari.api.entity.absence.AbsenceDay>>

    @Query("SELECT * FROM ${APIBaseKeys.ABSENCE_DAY} WHERE date=:date")
    abstract suspend fun getDay(date: ZonedDateTime): cz.lastaapps.bakalari.api.entity.absence.AbsenceDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDays(list: List<cz.lastaapps.bakalari.api.entity.absence.AbsenceDay>)

    @Query("DELETE FROM ${APIBaseKeys.ABSENCE_DAY}")
    protected abstract suspend fun deleteDays()

    @Transaction
    open suspend fun replaceDays(list: List<cz.lastaapps.bakalari.api.entity.absence.AbsenceDay>) {
        deleteDays()
        insertDays(list)
    }


    @Query("SELECT * FROM ${APIBaseKeys.ABSENCE_SUBJECT}")
    abstract fun getSubjects(): Flow<List<cz.lastaapps.bakalari.api.entity.absence.AbsenceSubject>>

    @Query("SELECT * FROM ${APIBaseKeys.ABSENCE_SUBJECT} WHERE name=:subjectName")
    abstract suspend fun getSubject(subjectName: String): cz.lastaapps.bakalari.api.entity.absence.AbsenceSubject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSubjects(list: List<cz.lastaapps.bakalari.api.entity.absence.AbsenceSubject>)

    @Query("DELETE FROM ${APIBaseKeys.ABSENCE_SUBJECT}")
    protected abstract suspend fun deleteSubjects()

    @Transaction
    open suspend fun replaceSubjects(list: List<cz.lastaapps.bakalari.api.entity.absence.AbsenceSubject>) {
        deleteSubjects()
        insertSubjects(list)
    }

    @Transaction
    open suspend fun deleteAll() {
        deleteThreshold()
        deleteDays()
        deleteSubjects()
    }
}