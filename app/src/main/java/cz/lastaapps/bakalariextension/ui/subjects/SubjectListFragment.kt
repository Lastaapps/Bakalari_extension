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

package cz.lastaapps.bakalariextension.ui.subjects

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.MobileNavigationDirections
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.subjects.data.Subject
import cz.lastaapps.bakalariextension.databinding.TemplateLoadingListBinding
import cz.lastaapps.bakalariextension.ui.BasicRecyclerAdapter

typealias SubjectAdapter = BasicRecyclerAdapter<Subject>

/**Shows subjects and on click their info*/
class SubjectListFragment : Fragment() {
    companion object {
        private val TAG = SubjectListFragment::class.java.simpleName
    }

    private lateinit var binding: TemplateLoadingListBinding
    private val viewModel: SubjectViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        //views setup
        binding =
            DataBindingUtil.inflate(inflater, R.layout.template_loading_list, container, false)
        binding.viewmodel = viewModel
        binding.setLifecycleOwner { lifecycle }

        //sets up RecycleView
        binding.list.apply {
            setHasFixedSize(true)
            adapter = SubjectAdapter({ it.name }).apply {
                onItemClicked = { subjectClicked(it) }
            }
        }

        //loads data
        viewModel.subjects.observe({ lifecycle }) {
            showSubjects()
        }
        if (viewModel.subjects.value != null) {
            showSubjects()
        } else {
            viewModel.onRefresh()
        }

        return binding.root
    }

    /**updates adapter with new teacher data*/
    private fun showSubjects() {
        Log.i(TAG, "Data updated")

        (binding.list.adapter as SubjectAdapter)
            .update(viewModel.subjects.value!!)
    }

    /**shows subject info*/
    private fun subjectClicked(subject: Subject) {
        requireActivity().findNavController(R.id.nav_host_fragment).navigate(
            MobileNavigationDirections.actionSubjectInfo(subject.id)
        )
    }
}