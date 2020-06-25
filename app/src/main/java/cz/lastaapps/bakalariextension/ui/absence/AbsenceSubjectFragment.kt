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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceRoot
import cz.lastaapps.bakalariextension.databinding.FragmentAbsenceSubjectBinding

class AbsenceSubjectFragment : Fragment() {

    private val viewModel: AbsenceViewModel by activityViewModels()
    private lateinit var binding: FragmentAbsenceSubjectBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_absence_subject, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.viewmodel = viewModel

        binding.list.adapter = AbsenceSubjectAdapter()

        viewModel.executeOrRefresh(lifecycle) { updateData(it) }

        return binding.root
    }

    private fun updateData(absenceRoot: AbsenceRoot) {
        (binding.list.adapter as AbsenceSubjectAdapter).update(absenceRoot)
    }
}