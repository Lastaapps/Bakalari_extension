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

package cz.lastaapps.bakalariextension.ui.absence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceRoot
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceSubject
import cz.lastaapps.bakalariextension.databinding.EntryAbsenceSubjectBinding

class AbsenceSubjectAdapter :
    RecyclerView.Adapter<AbsenceSubjectAdapter.BindingHolder> {

    init {
        setHasStableIds(true)
    }

    private var threshHold: Double
    private var list: DataIdList<AbsenceSubject>

    constructor(root: AbsenceRoot) : this(root.percentageThreshold, root.subjects)

    constructor(
        threshHold: Double = 0.0,
        list: DataIdList<AbsenceSubject> = DataIdList()
    ) : super() {
        this.threshHold = threshHold
        this.list = list
    }

    class BindingHolder(val binding: EntryAbsenceSubjectBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        return BindingHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.entry_absence_subject,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        val binding = holder.binding

        binding.threshold = threshHold
        binding.subject = list[position]
    }

    fun update(root: AbsenceRoot) {
        update(root.percentageThreshold, root.subjects)
    }

    fun update(threshHold: Double = 0.0, list: DataIdList<AbsenceSubject> = DataIdList()) {
        this.threshHold = threshHold
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return list[position].id.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}