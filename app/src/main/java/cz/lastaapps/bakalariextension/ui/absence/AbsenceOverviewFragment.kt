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
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.FragmentOverviewBinding

/**shows how many lesson has user unexcused or ok text when everything is ok*/
class AbsenceOverviewFragment : Fragment() {

    companion object {
        private val TAG = AbsenceOverviewFragment::class.simpleName
    }

    private lateinit var binding: FragmentOverviewBinding
    private val viewModel: AbsenceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //inflates views
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_overview,
                container,
                false
            )
        binding.lifecycleOwner = LifecycleOwner { lifecycle }
        binding.viewmodel = viewModel
        binding.drawable = R.drawable.module_absence
        //TODO contentDescription
        binding.contentDescription = ""

        //navigates to homework fragments
        binding.contentLayout.setOnClickListener {
            it.findNavController().navigate(R.id.nav_absence)
        }

        //starts marks loading if they aren't yet
        viewModel.executeOrRefresh(lifecycle) { dataChanged() }

        return binding.root
    }

    /**sets actual content of the fragment*/
    private fun dataChanged() {
        Log.i(TAG, "Data changed, updating")

        val days = viewModel.requireData().days
        var unsolved = 0

        for (day in days) {
            unsolved += day.unsolved
        }

        val text = if (unsolved > 0) {
            resources.getQuantityString(R.plurals.absence_overview_template, unsolved, unsolved)
        } else {
            getString(R.string.absence_overview_ok)
        }

        binding.text.text = text
    }
}