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

package cz.lastaapps.bakalariextension.api.subjects.data

import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.subjects.SubjectList
import cz.lastaapps.bakalariextension.api.subjects.TeacherList

class Teacher(
    id: String,
    val name: String,
    val shortcut: String,
    val email: String,
    val web: String,
    val phoneSchool: String,
    val phoneHome: String,
    val phoneMobile: String
) : DataId<String>(id), Comparable<Teacher> {

    override fun compareTo(other: Teacher): Int {
        return name.compareTo(other.name)
    }

    companion object {

        /**Gets teacher list from subjects*/
        fun subjectsToTeachers(subjectList: SubjectList): TeacherList {
            return TeacherList(HashSet<Teacher>().apply {
                subjectList.forEach {
                    add(it.teacher)
                }
            }.toList().sorted())
        }

        /**@return only subjects thought by this teacher*/
        fun getTeachersSubjects(subjectList: SubjectList, teacher: Teacher): SubjectList {
            return SubjectList().apply {
                for (subject in subjectList) {
                    if (subject.teacher == teacher) {
                        add(subject)
                    }
                }
            }
        }
    }
}