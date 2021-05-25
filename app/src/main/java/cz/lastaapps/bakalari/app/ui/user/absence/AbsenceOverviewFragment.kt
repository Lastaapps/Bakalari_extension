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
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import cz.lastaapps.bakalari.api.core.DataIdList
import cz.lastaapps.bakalari.api.core.absence.holders.AbsenceDay
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.TemplateOverviewBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels

/**shows how many lesson has user unexcused or ok text when everything is ok*/
class AbsenceOverviewFragment : Fragment() {

    companion object {
        private val TAG = AbsenceOverviewFragment::class.simpleName
    }

    private lateinit var binding: TemplateOverviewBinding
    private val viewModel: AbsenceViewModel by accountsViewModels()

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
                R.layout.template_overview,
                container,
                false
            )
        binding.lifecycleOwner = LifecycleOwner { lifecycle }
        binding.viewModel = viewModel
        binding.drawable = R.drawable.module_absence
        //TODO contentDescription
        binding.contentDescription = ""

        //navigates to homework fragments
        binding.contentLayout.setOnClickListener {
            it.findNavController().navigate(R.id.nav_absence)
        }

        //starts marks loading if they aren't yet
        viewModel.runOrRefresh(viewModel.days, lifecycle) { dataChanged(it) }

        return binding.root
    }

    /**sets actual content of the fragment*/
    private fun dataChanged(days: DataIdList<AbsenceDay>) {
        Log.i(TAG, "Data changed, updating")

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