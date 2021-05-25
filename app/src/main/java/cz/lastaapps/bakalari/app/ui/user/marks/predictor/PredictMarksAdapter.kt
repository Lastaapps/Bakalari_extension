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

package cz.lastaapps.bakalari.app.ui.user.marks.predictor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.api.core.DataIdList
import cz.lastaapps.bakalari.api.core.marks.holders.Mark
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.MarksPredictorMarkBinding

class PredictMarksAdapter(
    private val allMarks: DataIdList<Mark>,
    private val onEdit: ((mark: Mark) -> Unit),
    private val onDelete: ((mark: Mark) -> Unit)
) :
    RecyclerView.Adapter<PredictMarksAdapter.DataBindingHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class DataBindingHolder(val binding: MarksPredictorMarkBinding) :
        RecyclerView.ViewHolder(binding.root)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingHolder {

        val inflater: LayoutInflater = LayoutInflater.from(parent.context)

        val binding: MarksPredictorMarkBinding =
            DataBindingUtil.inflate(inflater, R.layout.marks_predictor_mark, parent, false)

        return DataBindingHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: DataBindingHolder, position: Int) {

        val binding = holder.binding
        val mark = allMarks[position]
        binding.markData = mark

        binding.root.setOnClickListener {
            onEdit(mark)
        }

        binding.delete.setOnClickListener {
            onDelete(mark)
        }

    }

    // Return the size of your DataSet (invoked by the layout manager)
    override fun getItemCount() = allMarks.size
}