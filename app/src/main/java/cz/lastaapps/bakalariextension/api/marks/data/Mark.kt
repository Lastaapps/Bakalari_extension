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

package cz.lastaapps.bakalariextension.api.marks.data

import android.util.Log
import cz.lastaapps.bakalariextension.api.DataID
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.roundToInt

/**Represents mark, contains all mark info*/
class Mark(
    var markDate: String,
    var editDate: String,
    /**What was test about*/
    var caption: String,
    var theme: String,
    /**marks value to show, 1, 1-,... or number of points like 0,69*/
    var markText: String,
    var teacherId: String,
    /**Type shortcut of mark - T, E, H,...*/
    var type: String,
    /**Type of mark - test, examination, homework,...*/
    var typeNote: String,
    var weight: Int,
    var subjectId: String,
    var isNew: Boolean,
    var isPoints: Boolean,
    var calculatedMarkText: String,
    var classRankText: String,
    id: String,
    /**Does not contain number of points*/
    var pointsText: String,
    var maxPoints: Int
) : DataID<String>(id), Comparable<Mark> {

    /**Compares marks by date and if mark is new*/
    override fun compareTo(other: Mark): Int {
        if (isNew != other.isNew) {
            return if (isNew) 1 else -1
        }
        return toDate().compareTo(other.toDate())
    }

    /**Cashes parsed date*/
    private var _toDate: ZonedDateTime? = null

    /**@return parsed date of mark*/
    fun toDate(): ZonedDateTime {
        if (_toDate == null)
            _toDate = TimeTools.parse(markDate, TimeTools.COMPLETE_FORMAT)
        return _toDate!!
    }

    /**@return marks date in format dd.MM.yyyy*/
    fun simpleDate(): String {
        return TimeTools.format(toDate(), "dd.MM.yyyy")
    }

    /**Cashes parsed edited date*/
    private var _toEditDate: ZonedDateTime? = null

    /**@return parsed date of mark*/
    fun toEditDate(): ZonedDateTime {
        if (_toEditDate == null)
            _toEditDate = TimeTools.parse(editDate, TimeTools.COMPLETE_FORMAT)
        return _toEditDate!!
    }

    /**@return If mark should be shown as new*/
    fun showAsNew(): Boolean {
        val duration = Duration.between(
            TimeTools.toMidnight(toEditDate()),
            TimeTools.toMidnight(TimeTools.now)
        )
        return duration.toDays() < MySettings.withAppContext().getNewMarkDuration()
    }

    companion object {

        private val TAG = Mark::class.java.simpleName

        /**generates mark with some default data*/
        val default: Mark
            get() = Mark(
                TimeTools.format(TimeTools.now, TimeTools.COMPLETE_FORMAT),
                TimeTools.format(TimeTools.now.minusYears(10), TimeTools.COMPLETE_FORMAT),
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
                    marks.isEmpty() -> 0

                    //normal marks
                    isAllNormal(marks) -> {
                        var average = 0.0
                        var weights = 0

                        for (mark in marks) {
                            //for marks like A, S, etc.
                            try {
                                average += parseMarkValue(mark.markText) * mark.weight
                                weights += mark.weight
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
                                average += mark.markText.toDouble() / mark.maxPoints * mark.weight
                                weights += mark.weight
                            } catch (e: Exception) {
                            }
                        }

                        average /= weights

                        (average * 100.0).roundToInt() / 100.0
                    }

                    //mixed marks
                    else -> {
                        0
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