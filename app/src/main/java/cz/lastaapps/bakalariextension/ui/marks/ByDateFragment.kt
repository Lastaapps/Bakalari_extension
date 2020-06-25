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
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.LoadingListTemplateBinding

/**Shows marks from all subjects sorted by date
 * placed in MarksRootFragment*/
class ByDateFragment : Fragment() {
    companion object {
        private val TAG = ByDateFragment::class.java.simpleName
    }

    //root view
    lateinit var binding: LoadingListTemplateBinding

    //ViewModel with marks
    val viewModel: MarksViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //inflates view
        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating view")
            binding =
                DataBindingUtil.inflate(inflater, R.layout.loading_list_template, container, false)
            binding.apply {
                viewmodel = viewModel
                setLifecycleOwner { lifecycle }
                list.adapter = MarksAdapter()
            }

            viewModel.marks.observe({ lifecycle }) { showMarks() }
            if (viewModel.marks.value != null) {
                showMarks()
            } else {
                viewModel.onRefresh()
            }
        } else {
            Log.i(TAG, "Already created")
        }

        return binding.root
    }

    /**puts marks into view*/
    private fun showMarks() {
        Log.i(TAG, "Updating based on new marks")

        (binding.list.adapter as MarksAdapter)
            .updateMarksRoot(viewModel.marks.value!!)
    }
}

