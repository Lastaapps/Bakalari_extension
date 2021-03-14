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

package cz.lastaapps.bakalari.app.ui.user.marks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.marks.data.MarksList
import cz.lastaapps.bakalari.app.api.marks.data.MarksPairList
import cz.lastaapps.bakalari.app.api.marks.data.findSubject
import cz.lastaapps.bakalari.app.databinding.MarksRowBinding

/**Adapter for RecycleView containing marks*/
class MarksAdapter : RecyclerView.Adapter<MarksAdapter.DataBindingHolder> {

    init {
        setHasStableIds(true)
    }

    /**all subjects*/
    private var pairs: MarksPairList? = null

    /**marks to show*/
    private var marks: MarksList

    /**Shows selected marks only*/
    constructor(marks: MarksList = MarksList()) : super() {
        this.marks = marks
    }

    /**Shows selected marks and subject*/
    constructor(
        pairs: MarksPairList,
        marks: MarksList
    ) {
        this.pairs = pairs
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

        binding.subjectData = null

        //shows subject if available
        pairs?.let {
            binding.subjectData = it.findSubject(mark.subjectId)?.subject
        }
    }

    fun updateMarks(marks: MarksList) {
        this.marks = marks
        notifyDataSetChanged()
    }

    fun updatePairs(pairs: MarksPairList?, marks: MarksList) {
        this.pairs = pairs
        this.marks = marks
        notifyDataSetChanged()
    }

    /**@return The number of marks*/
    override fun getItemCount() = marks.size

    override fun getItemId(position: Int): Long {
        return marks[position].id.hashCode().toLong()
    }
}