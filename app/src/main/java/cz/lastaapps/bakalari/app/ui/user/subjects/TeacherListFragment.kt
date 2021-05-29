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

package cz.lastaapps.bakalari.app.ui.user.subjects

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import cz.lastaapps.bakalari.api.entity.subjects.Teacher
import cz.lastaapps.bakalari.app.NavGraphUserDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.TemplateLoadingListBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.tools.ui.BasicRecyclerAdapter

typealias TeacherAdapter = BasicRecyclerAdapter<Teacher>

/**Shows list of all teachers*/
class TeacherListFragment : Fragment() {

    companion object {
        private val TAG = TeacherListFragment::class.java.simpleName
    }

    private lateinit var binding: TemplateLoadingListBinding
    private val viewModel: SubjectViewModel by accountsViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        //views setup
        binding =
            DataBindingUtil.inflate(inflater, R.layout.template_loading_list, container, false)
        binding.viewModel = viewModel
        binding.setLifecycleOwner { lifecycle }

        //teacher list setup
        binding.list.apply {
            setHasFixedSize(true)
            adapter = cz.lastaapps.bakalari.app.ui.user.subjects.TeacherAdapter({ it.name }).apply {
                onItemClicked = { teacherClicked(it) }
            }
        }

        //loads data
        viewModel.executeTeachersOrRefresh(lifecycle) { showTeachers() }

        return binding.root
    }

    /**updates adapter with new teacher data*/
    private fun showTeachers() {
        Log.i(TAG, "Data updated")

        (binding.list.adapter as TeacherAdapter)
            .update(viewModel.teachers.value!!)
    }

    /**teacher's name was selected*/
    private fun teacherClicked(teacher: Teacher) {
        requireActivity().findNavController(R.id.nav_host_fragment).navigate(
            NavGraphUserDirections.actionTeacherInfo(teacher.id)
        )
    }
}