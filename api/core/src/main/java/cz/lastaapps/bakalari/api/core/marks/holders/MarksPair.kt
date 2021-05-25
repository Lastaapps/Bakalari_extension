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

package cz.lastaapps.bakalari.api.core.marks.holders

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation

data class MarksPair(
    @Embedded
    val subject: MarksSubject,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId",
        entity = Mark::class,
    )
    private val m: List<Mark>
) {
    @Ignore
    val marks = cz.lastaapps.bakalari.api.core.marks.holders.MarksList(m)
}

@Deprecated(message = "Use database instead")
fun MarksPairList.findSubject(subjectId: String): MarksSubject? {
    for (pair in this) {
        val subject = pair.subject
        if (subject.subject.id == subjectId)
            return subject
    }
    return null
}

@Deprecated(message = "Use database instead")
fun MarksPairList.allMarks(): MarksList =
    cz.lastaapps.bakalari.api.core.marks.holders.MarksList().also { list ->
        for (pair in this)
            list.addAll(pair.marks)
    }

@Deprecated(message = "Use database instead")
fun MarksPairList.subjects(): MarksSubjectList =
    MarksSubjectList().also { list ->
        for (pair in this)
            list.add(pair.subject)
    }

