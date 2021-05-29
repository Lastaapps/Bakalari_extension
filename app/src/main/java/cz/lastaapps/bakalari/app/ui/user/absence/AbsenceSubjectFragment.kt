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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalari.api.entity.absence.AbsenceRoot
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentAbsenceSubjectBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels

class AbsenceSubjectFragment : Fragment() {

    private val viewModel: AbsenceViewModel by accountsViewModels()
    private lateinit var binding: FragmentAbsenceSubjectBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_absence_subject, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.viewModel = viewModel

        binding.list.adapter = AbsenceSubjectAdapter()

        viewModel.runOrRefresh(viewModel.root, lifecycle) { updateData(it) }

        return binding.root
    }

    private fun updateData(absenceRoot: AbsenceRoot) {
        (binding.list.adapter as AbsenceSubjectAdapter).update(absenceRoot)
    }
}