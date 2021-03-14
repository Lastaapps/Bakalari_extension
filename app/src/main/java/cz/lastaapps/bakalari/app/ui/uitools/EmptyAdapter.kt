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

package cz.lastaapps.bakalari.app.ui.uitools

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.platform.App

class EmptyAdapter<T : RecyclerView.ViewHolder>(
    private val adapter: RecyclerView.Adapter<T>,
    var emptyMessage: String = App.getString(R.string.no_items)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ID_EMPTY = Long.MIN_VALUE
        const val TYPE_EMPTY = Int.MIN_VALUE

        /**@return adapter hidden inside EmptyAdapter casted to the proper value*/
        fun <T : RecyclerView.Adapter<*>> getAdapter(recyclerView: RecyclerView): T {
            return (recyclerView.adapter as EmptyAdapter<*>).run {
                notifyDataSetChanged()
                adapter as T
            }
        }
    }

    private var isEmpty: Boolean = adapter.itemCount == 0

    var enabled = true

    private fun canShow() = isEmpty && enabled

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == TYPE_EMPTY) {
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.empty_message, parent, false)
            ) {}

        } else {
            adapter.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return if (holder.itemViewType == TYPE_EMPTY) {
            holder.itemView.findViewById<TextView>(R.id.text).text = emptyMessage

        } else {
            adapter.onBindViewHolder(holder as T, position)
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

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder.itemViewType == TYPE_EMPTY)
            super.onBindViewHolder(holder, position, payloads)
        else {
            adapter.onBindViewHolder(holder as T, position, payloads)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        adapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        adapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return if (holder.itemViewType == TYPE_EMPTY)
            super.onFailedToRecycleView(holder)
        else {
            adapter.onFailedToRecycleView(holder as T)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder.itemViewType == TYPE_EMPTY)
            super.onViewAttachedToWindow(holder)
        else {
            adapter.onViewAttachedToWindow(holder as T)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder.itemViewType == TYPE_EMPTY)
            super.onViewDetachedFromWindow(holder)
        else {
            adapter.onViewDetachedFromWindow(holder as T)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {

        if (holder.itemViewType == TYPE_EMPTY)
            super.onViewRecycled(holder)
        else {
            adapter.onViewRecycled(holder as T)
        }
    }
}