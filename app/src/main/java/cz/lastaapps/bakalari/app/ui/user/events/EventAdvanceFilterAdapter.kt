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

package cz.lastaapps.bakalari.app.ui.user.events

import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.setMargins
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.SimpleData
import cz.lastaapps.bakalari.app.ui.uitools.LifecycleAdapter
import cz.lastaapps.bakalari.app.ui.uitools.LifecycleViewHolder

/**Creates selection to filter items like all, school, free time,...*/
class EventAdvanceFilterAdapter(
    var list: List<SimpleData>,
    var selectAllLiveData: (() -> MutableLiveData<Boolean>),
    var getLiveData: ((type: SimpleData) -> MutableLiveData<Boolean>)
) : LifecycleAdapter<LifecycleViewHolder>() {

    companion object {
        private const val TYPE_ALL = 0
        private const val TYPE_OTHER = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LifecycleViewHolder {
        return object : LifecycleViewHolder(AppCompatCheckBox(parent.context).apply {
            (layoutParams as? ViewGroup.MarginLayoutParams)
                ?.setMargins(resources.getDimensionPixelSize(R.dimen.margin_basic))
        }) {}
    }

    /**Uses LifeData to save and sync data, logic contained in the ViewModel*/
    override fun onBindViewHolder(holder: LifecycleViewHolder, position: Int) {
        val liveData = when (holder.itemViewType) {
            TYPE_ALL -> {
                selectAllLiveData()
            }
            TYPE_OTHER -> {
                getLiveData(list[position - 1])
            }
            else -> return
        }
        val checkBox = holder.itemView as AppCompatCheckBox

        checkBox.text =
            if (position == 0)
                checkBox.context.getString(R.string.events_select_all)
            else
                list[position - 1].name

        liveData.observe(holder) {
            checkBox.isChecked = it
        }
        checkBox.setOnClickListener {
            liveData.value = checkBox.isChecked
        }
    }

    override fun getItemCount(): Int {
        return 1 + list.size
    }

    fun update(list: List<SimpleData>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return if (position == 0) 0 else list[position - 1].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_ALL else TYPE_OTHER
    }
}