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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import cz.lastaapps.bakalari.api.entity.marks.MarksPairList
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.TemplateLoadingListBinding

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
            binding.also {
                it.viewModel = viewModel
                it.lifecycleOwner = LifecycleOwner { lifecycle }

                it.list.adapter = SubjectAdapter()
            }

            viewModel.runOrRefresh(viewModel.pairs, lifecycle) { showSubjects(it) }

        } else {
            Log.i(TAG, "View already created")
        }

        return binding.root
    }

    /**fills up list with subjects*/
    private fun showSubjects(pairs: MarksPairList) {
        Log.i(TAG, "Updating based on new marks")

        (binding.list.adapter as SubjectAdapter)
            .update(pairs)
    }
}