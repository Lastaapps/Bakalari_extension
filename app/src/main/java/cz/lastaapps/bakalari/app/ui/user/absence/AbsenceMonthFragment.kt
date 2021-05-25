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
import androidx.lifecycle.lifecycleScope
import cz.lastaapps.bakalari.api.core.DataIdList
import cz.lastaapps.bakalari.api.core.absence.holders.AbsenceMonth
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentAbsenceDayBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import kotlinx.coroutines.launch

class AbsenceMonthFragment : Fragment() {

    companion object {
        private val TAG = AbsenceMonthFragment::class.java.simpleName
    }

    val viewModel: AbsenceViewModel by accountsViewModels()
    val userViewModel: UserViewModel by accountsViewModels()
    lateinit var binding: FragmentAbsenceDayBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_absence_day, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.viewModel = viewModel
        binding.legendTypeString = getString(R.string.absence_date)

        binding.list.adapter = AbsenceDayAdapter()
        binding.legend.setOnClickListener { showLegend() }

        lifecycleScope.launch {
            val user = userViewModel.waitForData()
            viewModel.runOrRefresh(
                viewModel.getMonths(user.firstSeptember),
                lifecycle
            ) { updateData(it) }
        }

        return binding.root
    }

    private fun updateData(months: DataIdList<AbsenceMonth>) {
        Log.i(TAG, "Updating data")

        (binding.list.adapter as AbsenceDayAdapter).update(months)
    }

    private fun showLegend() {
        Log.i(TAG, "Showing legend")

        AbsenceLegendFragment().show(childFragmentManager, TAG + "_legend")
    }
}