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

package cz.lastaapps.bakalariextension.ui.homework

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.HomeworkEntryBinding

/** Adapter to show homework list - uses homework_entry layout*/
class HmwAdapter(var list: DataIdList<Homework>, private val activity: AppCompatActivity) :
    RecyclerView.Adapter<HmwAdapter.BindingHolder>() {

    /**holds binding instead of view*/
    class BindingHolder(var binding: HomeworkEntryBinding) : RecyclerView.ViewHolder(binding.root)

    /**inflates binding and saves it into holder*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        return BindingHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.homework_entry,
                parent,
                false
            )
        )
    }

    /**updates binding with data*/
    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        val binding = holder.binding

        val homework = list[position]
        binding.hw = homework
        binding.mgr = HmwEntryManager(activity, binding, homework)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}