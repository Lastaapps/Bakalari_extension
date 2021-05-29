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

package cz.lastaapps.bakalari.app.ui.user.themes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.api.entity.themes.ThemeList
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.EntryThemeBinding

/**Adapter showing themes*/
class ThemeAdapter(var themeList: ThemeList = ThemeList()) :
    RecyclerView.Adapter<ThemeAdapter.BindingHolder>() {

    init {
        setHasStableIds(true)
    }

    class BindingHolder(val binding: EntryThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        return BindingHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.entry_theme,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        holder.binding.theme = themeList[position]
    }

    fun update(list: ThemeList) {
        themeList = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return themeList.size
    }

    override fun getItemId(position: Int): Long {
        return themeList[position].id.hashCode().toLong()
    }
}