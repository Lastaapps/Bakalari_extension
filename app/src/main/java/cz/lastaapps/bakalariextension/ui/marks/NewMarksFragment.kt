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

package cz.lastaapps.bakalariextension.ui.marks

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
import cz.lastaapps.bakalariextension.databinding.FragmentMarksNewBinding

/**Fragment shown in HomeFragment
 * shown new marks*/
class NewMarksFragment : Fragment() {
    companion object {
        private val TAG = NewMarksFragment::class.java.simpleName
    }

    //views
    lateinit var binding: FragmentMarksNewBinding

    //data - marks
    val viewModel: MarksViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //creates view
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_marks_new, container, false)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = LifecycleOwner { lifecycle }

        binding.drawable = R.drawable.module_marks
        //TODO contentDescription
        binding.contentDescription = ""

        binding.list.adapter = MarksAdapter()

        //navigates to homework fragments
        binding.contentLayout.setOnClickListener {
            it.findNavController().navigate(R.id.nav_marks)
        }

        //starts marks loading if they aren't yet
        viewModel.executeOrRefresh(lifecycle) { dataChanged() }

        return binding.root
    }

    /**sets actual content of the fragment*/
    private fun dataChanged() {
        Log.i(TAG, "Data changed, updating")

        val marks = viewModel.requireData()
        val newMarks = marks.getNewMarks()

        val text = if (newMarks.isNotEmpty()) {
            resources.getQuantityString(
                R.plurals.marks_new_template,
                newMarks.size,
                newMarks.size
            )
        } else {
            getString(R.string.marks_new_no_marks)
        }

        binding.text.text = text

        //puts views in
        (binding.list.adapter as MarksAdapter).updateMarksRoot(marks, newMarks)
    }
}