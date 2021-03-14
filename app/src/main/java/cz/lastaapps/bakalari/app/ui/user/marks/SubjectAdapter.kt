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
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.marks.data.MarksPairList
import cz.lastaapps.bakalari.app.databinding.MarksSubjectBinding

class SubjectAdapter(private var pairs: MarksPairList = MarksPairList()) :
    RecyclerView.Adapter<SubjectAdapter.DataBindingHolder>() {

    init {
        setHasStableIds(true)
    }

    /**Holds binding with view*/
    class DataBindingHolder(val binding: MarksSubjectBinding) :
        RecyclerView.ViewHolder(binding.root)


    /**Creates new ViewHolder*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingHolder {

        val inflater: LayoutInflater = LayoutInflater.from(parent.context)

        //makes binding with views
        val binding: MarksSubjectBinding =
            DataBindingUtil.inflate(inflater, R.layout.marks_subject, parent, false)

        binding.list.adapter = MarksAdapter()

        //creates holder holding binding
        return DataBindingHolder(binding)
    }

    /**Updates views with valid data*/
    override fun onBindViewHolder(holder: DataBindingHolder, position: Int) {

        //gets subject data object
        val pair = pairs[position]

        //puts valid data in views
        val binding = holder.binding
        binding.pair = pair

        //inits list with subjects
        val list = binding.list
        (list.adapter as MarksAdapter).updateMarks(pair.marks)

        list.visibility = View.GONE

        //shows/hides subject's marks
        binding.root.setOnClickListener {
            list.visibility =
                if (list.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE
        }
    }

    fun update(marks: MarksPairList) {
        this.pairs = marks
        notifyDataSetChanged()
    }

    /**@return The number of subjects*/
    override fun getItemCount() = pairs.size

    override fun getItemId(position: Int): Long {
        return pairs[position].subject.subject.id.hashCode().toLong()
    }
}