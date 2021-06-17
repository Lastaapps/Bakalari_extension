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

package cz.lastaapps.bakalari.app.ui.user.homework

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalari.api.entity.homework.HomeworkList
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.TemplateLoadingListBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.tools.ui.EmptyAdapter

/** Shows current and not done yet homework list*/
class HmwCurrentFragment : Fragment() {
    companion object {
        private val TAG get() = HmwCurrentFragment::class.java.simpleName
    }

    lateinit var binding: TemplateLoadingListBinding
    val viewModel: HmwViewModel by accountsViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        //inflates view
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.template_loading_list,
            container,
            false
        )

        //setup
        binding.setLifecycleOwner { lifecycle }
        binding.viewModel = viewModel

        binding.list.adapter = EmptyAdapter(HmwAdapter(requireActivity() as AppCompatActivity))

        viewModel.runOrRefresh(viewModel.current, lifecycle) { updateHomework(it) }

        return binding.root
    }

    /**shows homework list*/
    private fun updateHomework(data: HomeworkList) {
        EmptyAdapter.getAdapter<HmwAdapter>(binding.list).update(data)

        scrollToHomework(data)
    }

    /**If there was request to show specific homework, scrolls there*/
    private fun scrollToHomework(list: HomeworkList) {

        viewModel.selectedHomeworkId.value?.let {

            Log.i(TAG, "Scrolling to selected homework")

            val index = list.getIndexById(it)
            if (index >= 0) {
                viewModel.selectedHomeworkId.value = null
                binding.list.scrollToPosition(index)
            } //else the id if from oder homework list
        }
    }
}