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

package cz.lastaapps.bakalariextension.ui.marks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.databinding.MarksRowBinding

/**Adapter for RecycleView containing marks*/
class MarksAdapter : RecyclerView.Adapter<MarksAdapter.DataBindingHolder> {

    /**all subjects*/
    private var marksAllSubjects: MarksAllSubjects? = null
    /**marks to show*/
    private val marks: DataIdList<Mark>

    /**Shows also subject name*/
    constructor(marksAllSubjects: MarksAllSubjects): super() {
        this.marksAllSubjects = marksAllSubjects
        this.marks = DataIdList(marksAllSubjects.getAllMarks().reversed())
    }

    /**Shows selected marks only*/
    constructor(marks: DataIdList<Mark>): super() {
        this.marks = marks
    }

    /**Shows selected marks and subject*/
    constructor(marksAllSubjects: MarksAllSubjects, marks: DataIdList<Mark>) {
        this.marksAllSubjects = marksAllSubjects
        this.marks = marks
    }

    /**Holds binding data with views*/
    class DataBindingHolder(val binding: MarksRowBinding) :
        RecyclerView.ViewHolder(binding.root)


    /**Creates new ViewHolder*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingHolder {

        val inflater: LayoutInflater = LayoutInflater.from(parent.context)

        //inflates views into binding
        val binding: MarksRowBinding =
            DataBindingUtil.inflate(inflater, R.layout.marks_row, parent, false)

        //creates holder
        return DataBindingHolder(binding)
    }

    /**Updates views with valid data*/
    override fun onBindViewHolder(holder: DataBindingHolder, position: Int) {

        //gets binding
        val binding = holder.binding

        //updates with valid data
        val mark = marks[position]
        binding.markData = mark

        //shows subject if available
        if (marksAllSubjects != null)
            binding.subjectData = marksAllSubjects!!.getSubjectForMark(mark)

    }

    /**@return The number of marks*/
    override fun getItemCount() = marks.size
}