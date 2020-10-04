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
import cz.lastaapps.bakalariextension.tools.searchNeutralText
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**defines Homework list to make code simpler to read*/
typealias HomeworkList = DataIdList<Homework>

/** All data from homework json*/
@Parcelize
data class Homework(
    override var id: String,
    var dateAward: ZonedDateTime,
    var dateControl: ZonedDateTime?,
    var dateDone: ZonedDateTime,
    var dateStart: ZonedDateTime,
    var dateEnd: ZonedDateTime,
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
        if (dateControl == null) dateControl = dateAward
    }

    override fun compareTo(other: Homework): Int {
        return dateEnd.compareTo(other.dateEnd)
    }

    companion object {

        /** Filters homework only from given subject*/
        fun filterBySubject(list: HomeworkList, subjectId: String): HomeworkList {
            return HomeworkList(list.filter { homework ->
                homework.subject.id == subjectId
            })
        }

        /** Filters homework only with given text in content or notice, ignores diacritics*/
        fun filterByText(list: HomeworkList, text: String): HomeworkList {
            return HomeworkList(list.filter { homework ->
                val content = homework.content.searchNeutralText()
                val notice = homework.notice.searchNeutralText()
                val toSearch = text.searchNeutralText()

                (content.contains(toSearch) || notice.contains(toSearch))
            })
        }
    }
}
