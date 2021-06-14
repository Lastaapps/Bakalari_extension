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

package cz.lastaapps.bakalari.app.ui.navigation.navview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.google.android.material.navigation.NavigationView
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.NavHeaderMainBinding
import cz.lastaapps.bakalari.app.databinding.NavHeaderProfileItemBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.uitools.observeForControllerGraphChanges
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.tools.ui.LifecycleAdapter
import cz.lastaapps.bakalari.tools.ui.LifecycleBindingHolder
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch

class NavHeaderController(fragmentActivity: FragmentActivity) {

    private val activity = fragmentActivity as MainActivity

    private val headerView: View =
        activity.findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
    private val headerBinding = DataBindingUtil.bind<NavHeaderMainBinding>(headerView)!!
    private val adapter = NavHeaderProfilesAdapter()

    private var userViewModel: UserViewModel? = null
    private val currentUser =
        CurrentUser.accountUUID.asFlow().asLiveData(activity.lifecycleScope.coroutineContext)

    init {
        headerBinding.apply {
            list.adapter = adapter
            adapter.onClick = { account ->
                activity.doAccountChange(account.uuid)
            }

            showAll.setOnClickListener { activity.doAccountChange(null) }

            stateSwitch.setOnClickListener { isStart = isStart != true }
        }

        observeForControllerGraphChanges(
            activity, {
                createViewModel()
                userViewModel?.data?.observe({ activity.lifecycle }) { user ->
                    headerBinding.user = user
                }

                currentUser.observe({ activity.lifecycle }) { uuid ->
                    uuid ?: return@observe

                    activity.lifecycleScope.launch {
                        val database = AccountsDatabase.getDatabase(activity)
                        val repo = database.repository
                        val current = repo.getByUUID(uuid)

                        val allOthers = repo.getAll().filter { item -> item != current }
                        headerBinding.account = current

                        adapter.update(allOthers)
                        //hides option to show list of no items
                        headerBinding.moreProfiles = allOthers.isNotEmpty()
                    }
                }
            }, {}, {
                headerBinding.user = null
                userViewModel?.data?.removeObservers { activity.lifecycle }
                userViewModel = null

                currentUser.removeObservers { activity.lifecycle }
                headerBinding.account = null
                adapter.update(emptyList())
            })
    }

    private fun createViewModel() {
        if (userViewModel == null) {
            val v: UserViewModel by activity.accountsViewModels()
            userViewModel = v
        }
    }
}

typealias ProfileHolder = LifecycleBindingHolder<NavHeaderProfileItemBinding>

private class NavHeaderProfilesAdapter(
    private var list: List<BakalariAccount> = ArrayList(),
    var onClick: ((BakalariAccount) -> Unit)? = null
) : LifecycleAdapter<ProfileHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder =
        ProfileHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.nav_header_profile_item,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
        val account = list[position]
        holder.binding.profile = account.toProfile()
        holder.binding.root.setOnClickListener { onClick?.let { it(account) } }
    }

    override fun getItemCount(): Int = list.size

    fun update(list: List<BakalariAccount>) {
        this.list = list
        notifyDataSetChanged()
    }
}