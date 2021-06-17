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

package cz.lastaapps.bakalari.app.ui.start.profiles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.EntryProfileBinding
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.tools.ui.LifecycleAdapter
import cz.lastaapps.bakalari.tools.ui.LifecycleBindingHolder

private typealias Holder = LifecycleBindingHolder<EntryProfileBinding>

class ProfileAdapter(
    private val viewModel: ProfilesViewModel,
    private var list: List<BakalariAccount> = ArrayList(),
    var onClick: ((view: View, item: BakalariAccount, position: Int, editing: Boolean) -> Unit)? = null
) : LifecycleAdapter<Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding: EntryProfileBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.entry_profile,
            parent,
            false
        )
        binding.viewModel = viewModel
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val account = list[position]
        val item = account.toProfile()
        holder.binding.profile = item

        val onClickListener: ((View) -> Unit) = { view ->
            onClick?.let { it(view, account, position, viewModel.editingMode.value!!) }
        }
        holder.binding.root.setOnClickListener(onClickListener)
        holder.binding.edit.setOnClickListener(onClickListener)
        holder.binding.delete.setOnClickListener(onClickListener)
        holder.binding.checkBox.setOnCheckedChangeListener { view, isChecked ->
            viewModel.autoLaunchChanged(isChecked, account)
        }
    }

    override fun getItemCount(): Int = list.size

    fun update(list: List<BakalariAccount>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long = list[position].uuid.hashCode().toLong()

}