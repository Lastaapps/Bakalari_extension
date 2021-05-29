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

package cz.lastaapps.bakalari.app.ui.user.absence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.api.entity.absence.AbsenceRoot
import cz.lastaapps.bakalari.api.entity.absence.AbsenceWrapper
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.EntryAbsenceDayBinding


class AbsenceDayAdapter :
    RecyclerView.Adapter<AbsenceDayAdapter.BindingHolder> {

    init {
        setHasStableIds(true)
    }

    private var list: DataIdList<AbsenceWrapper>

    constructor(root: AbsenceRoot) : this(root.days)

    constructor(list: DataIdList<*> = DataIdList<AbsenceWrapper>()) : super() {
        this.list = list as DataIdList<AbsenceWrapper>
    }

    class BindingHolder(val binding: EntryAbsenceDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        return BindingHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.entry_absence_day,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        val binding = holder.binding

        binding.day = list[position]
    }

    fun update(root: AbsenceRoot) {
        update(root.days)
    }

    fun update(list: DataIdList<*> = DataIdList<AbsenceWrapper>()) {
        this.list = list as DataIdList<AbsenceWrapper>
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return list[position].id.toLong()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

