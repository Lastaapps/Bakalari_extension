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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import cz.lastaapps.bakalari.api.entity.user.User
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.TemplateLoadingRootBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import cz.lastaapps.bakalari.tools.ui.lastUpdated


class AbsenceRootFragment : Fragment() {

    companion object {
        private val TAG = AbsenceRootFragment::class.java.simpleName
    }

    private val viewModel: AbsenceViewModel by accountsViewModels()
    private val userViewModel: UserViewModel by accountsViewModels()
    private lateinit var binding: TemplateLoadingRootBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        binding =
            DataBindingUtil.inflate(inflater, R.layout.template_loading_root, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.viewModel = viewModel

        //clears any other old adapter to prevent adapter not 'fresh' exception
        binding.pager.adapter = null
        binding.pager.offscreenPageLimit = 2

        //updates data based on user object
        userViewModel.runOrRefresh(lifecycle) { setupPager() }

        //updates last updated text
        viewModel.onDataUpdate(lifecycle) { showLastUpdated() }

        return binding.root
    }

    /**first we need to obtain user object to determinate witch fragments are enabled*/
    private fun setupPager() {

        userViewModel.runOrRefresh(lifecycle) { user ->
            AbsencePager(this, user.isFeatureEnabled(User.ABSENCE_SHOW_PERCENTAGE)).also {

                binding.pager.adapter = it

                TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->

                    tab.text = it.getPageTitle(position)
                }.attach()
            }
        }
    }

    /**updates last updated text in the bottom*/
    private fun showLastUpdated() {
        Log.i(TAG, "Updating last updated")

        var text = getString(R.string.absence_failed_to_load)
        val lastUpdated = viewModel.lastUpdated()
        lastUpdated?.let {
            text = lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}