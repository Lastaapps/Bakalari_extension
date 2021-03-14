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

package cz.lastaapps.bakalari.app.ui.user.marks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.marks.data.MarksList
import cz.lastaapps.bakalari.app.api.marks.data.MarksPairList
import cz.lastaapps.bakalari.app.databinding.FragmentMarksNewBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels

/**Fragment shown in HomeFragment
 * shown new marks*/
class NewMarksFragment : Fragment() {
    companion object {
        private val TAG = NewMarksFragment::class.java.simpleName
    }

    //views
    lateinit var binding: FragmentMarksNewBinding

    //data - marks
    val viewModel: MarksViewModel by accountsViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //creates view
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_marks_new, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = LifecycleOwner { lifecycle }

        binding.drawable = R.drawable.module_marks
        //TODO contentDescription
        binding.contentDescription = ""

        binding.list.adapter = MarksAdapter()

        //navigates to homework fragments
        binding.contentLayout.setOnClickListener {
            it.findNavController().navigate(R.id.nav_marks)
        }

        binding.showMore.setOnClickListener {
            binding.list.visibility = if (binding.list.visibility == View.VISIBLE)
                View.GONE else View.VISIBLE
        }

        //starts marks loading if they aren't yet
        viewModel.runOrRefresh(viewModel.newMarks, lifecycle) { newMarks ->
            viewModel.runOrRefresh(viewModel.pairs, lifecycle) { pairs ->
                dataChanged(pairs, newMarks)
            }
        }

        return binding.root
    }

    /**sets actual content of the fragment*/
    private fun dataChanged(pairs: MarksPairList, newMarks: MarksList) {
        Log.i(TAG, "Data changed, updating")

        val text = if (newMarks.isNotEmpty()) {
            binding.showMore.visibility = View.VISIBLE

            resources.getQuantityString(
                R.plurals.marks_new_template,
                newMarks.size,
                newMarks.size
            )
        } else {
            binding.showMore.visibility = View.GONE
            binding.list.visibility = View.GONE

            getString(R.string.marks_new_no_marks)
        }

        binding.text.text = text

        //puts views in
        (binding.list.adapter as MarksAdapter).updatePairs(pairs, newMarks)
    }
}