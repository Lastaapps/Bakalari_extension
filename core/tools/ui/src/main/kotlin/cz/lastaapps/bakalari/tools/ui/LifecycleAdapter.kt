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

package cz.lastaapps.bakalari.tools.ui

import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView

abstract class LifecycleAdapter<T : LifecycleViewHolder> : RecyclerView.Adapter<T>() {

    override fun onViewAttachedToWindow(holder: T) {
        super.onViewAttachedToWindow(holder)
        holder.onAppear()
    }

    override fun onViewDetachedFromWindow(holder: T) {
        super.onViewDetachedFromWindow(holder)
        //holder.onDisappear()
    }

    override fun onViewRecycled(holder: T) {
        super.onViewRecycled(holder)
        holder.onDisappear()
    }
}


open class LifecycleViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onAppear() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onDisappear() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        //lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}

open class LifecycleBindingHolder<T : ViewDataBinding>(val binding: T) :
    LifecycleViewHolder(binding.root) {
    init {
        binding.setLifecycleOwner { lifecycle }
    }
}
