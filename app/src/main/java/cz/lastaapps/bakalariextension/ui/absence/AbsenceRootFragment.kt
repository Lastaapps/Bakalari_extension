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

package cz.lastaapps.bakalariextension.ui.absence

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.absence.AbsenceStorage
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.databinding.LoadingRootTemplateBinding
import cz.lastaapps.bakalariextension.ui.UserViewModel

class AbsenceRootFragment : Fragment() {

    companion object {
        private val TAG = AbsenceRootFragment::class.java.simpleName
    }

    private val viewModel: AbsenceViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var binding: LoadingRootTemplateBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating view")

            binding =
                DataBindingUtil.inflate(inflater, R.layout.loading_root_template, container, false)
            binding.setLifecycleOwner { lifecycle }
            binding.viewmodel = viewModel

            binding.pager.isSaveEnabled = false

        } else
            Log.i(TAG, "Already created")

        //updates data based on user object
        userViewModel.executeOrRefresh(lifecycle) { setupPager() }

        //updates last updated text
        viewModel.executeOrRefresh(lifecycle) { showLastUpdated() }

        return binding.root
    }

    /**first we need to obtain user object to determinate witch fragments are enabled*/
    private fun setupPager() {
        AbsencePager(
            this,
            userViewModel.requireData().isFeatureEnabled(User.ABSENCE_SHOW_PERCENTAGE)
        ).also {

            binding.pager.adapter = it

            TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->

                tab.text = it.getPageTitle(position)
            }.attach()
        }
    }

    /**updates last updated text in the bottom*/
    private fun showLastUpdated() {
        Log.i(TAG, "Updating last updated")

        var text = getString(R.string.absence_failed_to_load)
        val lastUpdated = AbsenceStorage.lastUpdated()
        lastUpdated?.let {
            text = cz.lastaapps.bakalariextension.tools.lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}