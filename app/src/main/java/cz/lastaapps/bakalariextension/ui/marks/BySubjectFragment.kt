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
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.TemplateLoadingListBinding

/**Shows subjects and their marks
 * placed in MarksRootFragment*/
class BySubjectFragment : Fragment() {
    companion object {
        private val TAG = BySubjectFragment::class.java.simpleName
    }

    //views
    lateinit var binding: TemplateLoadingListBinding

    //ViewModel with views
    val viewModel: MarksViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //inflates views
        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating view")

            binding =
                DataBindingUtil.inflate(inflater, R.layout.template_loading_list, container, false)
            binding.apply {
                viewmodel = viewModel
                lifecycleOwner = LifecycleOwner { lifecycle }

                list.adapter = SubjectAdapter()
            }

            viewModel.executeOrRefresh(lifecycle) { showSubjects() }

        } else {
            Log.i(TAG, "View already created")
        }

        return binding.root
    }

    /**fills up list with subjects*/
    private fun showSubjects() {
        Log.i(TAG, "Updating based on new marks")

        (binding.list.adapter as SubjectAdapter)
            .update(viewModel.marks.value!!)
    }
}