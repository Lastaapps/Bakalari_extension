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

package cz.lastaapps.bakalari.api.entity.marks

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.core.DataId
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.entity.core.ModuleConfig
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toMidnight
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.roundToInt

typealias MarksList = DataIdList<Mark>
typealias MarksSubjectList = ArrayList<MarksSubject>
typealias MarksSubjectMarksLists = Pair<MarksSubjectList, MarksList>
typealias MarksPairList = ArrayList<MarksPair>

/**Represents mark, contains all mark info*/
@Parcelize
@Entity(tableName = APIBaseKeys.MARKS, inheritSuperIndices = true)
data class Mark(
    @ColumnInfo(index = true)
    val date: ZonedDateTime,
    @ColumnInfo(index = true)
    val editDate: ZonedDateTime,
    /**What was test about*/
    val caption: String,
    val theme: String,
    /**marks value to show, 1, 1-,... or number of points like 0,69*/
    val markText: String,
    val teacherId: String,
    /**Type shortcut of mark - T, E, H,...*/
    val type: String,
    /**Type of mark - test, examination, homework,...*/
    val typeNote: String,
    val weight: Int?,
    val subjectId: String,
    val isNew: Boolean,
    val isPoints: Boolean,
    val calculatedMarkText: String,
    val classRankText: String,
    @PrimaryKey
    @ColumnInfo(index = true)
    override var id: String,
    /**Does not contain number of points*/
    val pointsText: String,
    val maxPoints: Int
) : DataId<String>(id), Comparable<Mark> {

    /**Compares marks by date and if mark is new*/
    override fun compareTo(other: Mark): Int {
        if (isNew != other.isNew) {
            return if (isNew) 1 else -1
        }
        return -1 * date.compareTo(other.date)
    }

    /**@return marks date in format dd.MM.yyyy*/
    fun simpleDate(): String {
        return TimeTools.format(date, "d.M.yyyy")
    }

    /**@return If mark should be shown as new*/
    fun showAsNew(): Boolean {
        val duration = Duration.between(
            editDate.toMidnight(),
            TimeTools.now.toMidnight()
        )
        return duration.toDays() < ModuleConfig.getNewMarkDuration()
    }

    companion object {

        private val TAG get() = Mark::class.java.simpleName

        /**generates mark with some default data*/
        val default: Mark
            get() = Mark(
                TimeTools.now,
                TimeTools.now.minusYears(10),
                "Really hard test",
                "Super hard theme",
                "1",
                "12345",
                "T",
                "Test",
                4,
                "12345",
                false,
                false,
                "",
                "",
                Random().nextInt().toString(),
                "",
                100
            )

        /**@return marks average*/
        fun calculateAverage(marks: DataIdList<Mark>): String {

            Log.i(TAG, "Calculating marks average")

            return String.format(
                Locale("cs"), "%.2f", when {
                    //no marks
                    marks.isEmpty() -> 0.0

                    //normal marks
                    isAllNormal(marks) -> {
                        var average = 0.0
                        var weights = 0

                        for (mark in marks) {
                            //for marks like A, S, etc.
                            try {
                                average += parseMarkValue(mark.markText) * (mark.weight ?: 1)
                                weights += mark.weight ?: 1
                            } catch (e: Exception) {
                            }
                        }

                        average /= weights

                        (average * 100.0).roundToInt() / 100.0
                    }

                    //point marks
                    isAllPoints(marks) -> {
                        var average = 0.0
                        var weights = 0

                        for (mark in marks) {
                            try {
                                average += mark.markText.toDouble() / mark.maxPoints * (mark.weight
                                    ?: 1)
                                weights += mark.weight ?: 1
                            } catch (e: Exception) {
                            }
                        }

                        average /= weights

                        (average * 100.0).roundToInt() / 100.0
                    }

                    //mixed marks
                    else -> {
                        0.0
                    }
                }
            )
        }

        /**Made for normal marks with - at the end like 1-
         * @return value of mark, 1- is 1.5*/
        private fun parseMarkValue(markValue: String): Double {
            var mark = markValue[0].toString().toDouble()
            if (markValue.contains("-")) {
                mark += .5
            }
            return mark
        }

        /**@return if only normal marks are included*/
        fun isAllNormal(marks: DataIdList<Mark>): Boolean {
            for (mark in marks) {
                if (mark.isPoints) {
                    return false
                }
            }
            return true
        }

        /**@return if only points are included*/
        fun isAllPoints(marks: DataIdList<Mark>): Boolean {
            for (mark in marks) {
                if (!mark.isPoints) {
                    return false
                }
            }
            return true
        }

        /**@return If both points and normal marks are contained*/
        fun isMixed(marks: DataIdList<Mark>): Boolean {
            return (!isAllNormal(marks) && !isAllPoints(marks))
        }
    }
}