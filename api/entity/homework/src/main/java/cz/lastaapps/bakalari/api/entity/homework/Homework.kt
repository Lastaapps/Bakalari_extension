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

package cz.lastaapps.bakalari.api.entity.homework

import cz.lastaapps.bakalari.api.entity.attachment.Attachment
import cz.lastaapps.bakalari.api.entity.core.DataId
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.tools.searchNeutralText
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

/**defines Homework list to make code simpler to read*/
typealias HomeworkList = DataIdList<Homework>

/** All data from homework json*/
@Parcelize
data class Homework(
    override var id: String,
    val dateStart: ZonedDateTime,
    val dateEnd: ZonedDateTime,
    val content: String,
    val notice: String,
    val done: Boolean,
    val closed: Boolean,
    val electronic: Boolean,
    val finished: Boolean,
    val hour: Int,
    val classInfo: SimpleData,
    val group: SimpleData,
    val subject: SimpleData,
    val teacher: SimpleData,
    val attachments: DataIdList<Attachment>
) : DataId<String>(id), Comparable<Homework> {

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
