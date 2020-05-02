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

import cz.lastaapps.bakalariextension.api.DataIdList

/**List of all info about marks and their subjects*/
class MarksAllSubjects(
    val subjects: ArrayList<SubjectMarks>
) {

    //marks from all subjects
    private var _allMarks: DataIdList<Mark>? = null

    /**@return marks from all subjects sorted by date*/
    fun getAllMarks(): DataIdList<Mark> {
        //init only once
        if (_allMarks == null) {
            val dataIdList = DataIdList<Mark>()

            //puts all marks together
            for (subject in subjects) {
                for (mark in subject.marks) {
                    dataIdList.add(mark)
                }
            }
            dataIdList.sort()
            _allMarks = dataIdList
        }

        return _allMarks!!
    }

    /**@return subject info for mark given*/
    fun getSubjectForMark(mark: Mark): Subject? {
        //goes through subjects
        for (subject in subjects) {
            //checks if marks match
            if (subject.marks.contains(mark))
                return subject.subject
        }
        return null
    }
}