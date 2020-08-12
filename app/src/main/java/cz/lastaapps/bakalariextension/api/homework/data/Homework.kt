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

package cz.lastaapps.bakalariextension.api.homework.data

import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.attachment.data.Attachment
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.searchNeutralText
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**defines Homework list to make code simpler to read*/
typealias HomeworkList = DataIdList<Homework>

/** All data from homework json*/
@Parcelize
class Homework(
    override var id: String,
    var dateAward: String,
    var dateControl: String,
    var dateDone: String,
    var dateStart: String,
    var dateEnd: String,
    var content: String,
    var notice: String,
    var done: Boolean,
    var closed: Boolean,
    var electronic: Boolean,
    var hour: Int,
    var classInfo: SimpleData,
    var group: SimpleData,
    var subject: SimpleData,
    var teacher: SimpleData,
    var attachments: DataIdList<Attachment>
) : DataId<String>(id), Comparable<Homework> {
    init {
        if (dateControl == "") dateControl = dateAward
    }

    override fun compareTo(other: Homework): Int {
        return dateEndDate.compareTo(other.dateEndDate)
    }

    //parses and cashes dates from selection
    private var _dateAward: ZonedDateTime? = null
    val dateAwardDate: ZonedDateTime
        get() {
            if (_dateAward == null) _dateAward =
                TimeTools.parse(dateAward, TimeTools.COMPLETE_FORMAT)
            return _dateAward!!
        }

    private var _dateControl: ZonedDateTime? = null
    val dateDateControl: ZonedDateTime
        get() {
            if (_dateControl == null) _dateControl =
                TimeTools.parse(dateControl, TimeTools.COMPLETE_FORMAT)
            return _dateControl!!
        }


    private var _dateDone: ZonedDateTime? = null
    val dateDoneDate: ZonedDateTime
        get() {
            if (_dateDone == null) _dateDone = TimeTools.parse(dateDone, TimeTools.COMPLETE_FORMAT)
            return _dateDone!!
        }


    private var _dateStart: ZonedDateTime? = null
    val dateStartDate: ZonedDateTime
        get() {
            if (_dateStart == null) _dateStart =
                TimeTools.parse(dateStart, TimeTools.COMPLETE_FORMAT)
            return _dateStart!!
        }


    private var _dateEnd: ZonedDateTime? = null
    val dateEndDate: ZonedDateTime
        get() {
            if (_dateEnd == null) _dateEnd = TimeTools.parse(dateEnd, TimeTools.COMPLETE_FORMAT)
            return _dateEnd!!
        }


    companion object {

        /**Filters given homework list and filterers current and not finished yet ones*/
        fun getCurrent(list: HomeworkList): HomeworkList {
            val toReturn = HomeworkList()

            for (homework in list) {
                //haven't ended yet
                if (homework.dateEndDate.toLocalDate() >= TimeTools.today.toLocalDate()) {
                    toReturn.add(homework)
                } else
                //aren't marked as done
                    if (homework.dateEndDate.toLocalDate() < TimeTools.today.toLocalDate() && !homework.done) {
                        toReturn.add(homework)
                    }
            }

            return toReturn
        }

        /** Filters old and done homework list*/
        fun getOld(list: HomeworkList): HomeworkList {
            val toReturn = HomeworkList()

            for (homework in list) {
                if (homework.dateEndDate.toLocalDate() < TimeTools.today.toLocalDate() && homework.done) {
                    toReturn.add(homework)
                }
            }

            return toReturn
        }

        /** Filters homework only from given subject*/
        fun getBySubject(list: HomeworkList, subjectId: String): HomeworkList {
            return HomeworkList(list.filter { homework ->
                homework.subject.id == subjectId
            })
        }

        /** Filters homework only with given text in content or notice, ignores diacritics*/
        fun getByText(list: HomeworkList, text: String): HomeworkList {
            return HomeworkList(list.filter { homework ->
                val content = searchNeutralText(homework.content)
                val notice = searchNeutralText(homework.notice)
                val toSearch = searchNeutralText(text)

                (content.contains(toSearch) || notice.contains(toSearch))
            })
        }
    }
}
