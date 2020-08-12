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

package cz.lastaapps.bakalariextension.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R

class EmptyAdapter(
    val adapter: RecyclerView.Adapter<*>,
    var emptyMessage: String = App.getString(R.string.no_items)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ID_NO: Long = RecyclerView.NO_ID
        const val ID_EMPTY = Long.MIN_VALUE
        const val TYPE_EMPTY = -2
        const val TYPE_INVALID = RecyclerView.INVALID_TYPE
    }

    private var isEmpty: Boolean = adapter.itemCount == 0

    var enabled = true

    private fun canShow() = isEmpty && enabled

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == TYPE_EMPTY && isEmpty) {
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.empty_message, parent, false)
            ) {}

        } else {
            adapter.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return if (holder.itemViewType == TYPE_EMPTY && isEmpty) {
            holder.itemView.findViewById<TextView>(R.id.text).text = emptyMessage

        } else {
            adapter.run { onBindViewHolder(holder, position) }
        }
    }


    override fun getItemId(position: Int): Long {
        return if (canShow())
            ID_EMPTY
        else
            adapter.getItemId(position)
    }

    override fun getItemViewType(position: Int): Int {
        return if (canShow())
            TYPE_EMPTY
        else
            adapter.getItemViewType(position)
    }

    override fun getItemCount(): Int {
        isEmpty = adapter.itemCount == 0

        return if (canShow())
            1
        else
            adapter.itemCount
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        adapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (canShow())
            super.onBindViewHolder(holder, position, payloads)
        else {
            adapter.run { onBindViewHolder(holder, position, payloads) }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        adapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return if (canShow())
            super.onFailedToRecycleView(holder)
        else {
            adapter.run { onFailedToRecycleView(holder) }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (canShow())
            super.onViewAttachedToWindow(holder)
        else {
            adapter.run { onViewAttachedToWindow(holder) }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (canShow())
            super.onViewDetachedFromWindow(holder)
        else {
            adapter.run { onViewDetachedFromWindow(holder) }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {

        if (canShow())
            super.onViewRecycled(holder)
        else {
            adapter.run { onViewRecycled(holder) }
        }
    }
}