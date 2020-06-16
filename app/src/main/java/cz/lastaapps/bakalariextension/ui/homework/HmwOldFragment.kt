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

package cz.lastaapps.bakalariextension.ui.homework

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.databinding.LoadingListTemplateBinding

/**shows old homework list - done and outdated homework*/
class HmwOldFragment : Fragment() {
    companion object {
        private val TAG = HmwOldFragment::class.java.simpleName
    }

    lateinit var binding: LoadingListTemplateBinding
    val viewModel: HmwViewModel by activityViewModels()

    /**observes for marks update*/
    private var homeworkObserver = { _: HomeworkList ->
        Log.i(TAG, "Homework list updated")
        updateHomework()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //observers for homework list update
        viewModel.homework.observe({ lifecycle }, homeworkObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //inflates only layout once
        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating view")

            binding = DataBindingUtil.inflate(
                inflater,
                R.layout.loading_list_template,
                container,
                false
            )
            binding.apply {
                setLifecycleOwner { lifecycle }
                viewmodel = viewModel

                list.adapter = HmwAdapter(requireActivity() as AppCompatActivity)
            }

            //shows homework list
            updateHomework()
        } else {
            Log.i(TAG, "Already created")
        }

        return binding.root
    }

    /**shows homework list*/
    private fun updateHomework() {

        val oldHomework = Homework.getOld(viewModel.homework.value!!)

        binding.apply {
            (list.adapter as HmwAdapter).update(oldHomework)

            scrollToHomework(oldHomework)
        }
    }

    /**If there was request to show specific homework, scrolls there*/
    private fun scrollToHomework(homeworkList: HomeworkList) {
        Log.i(TAG, "Scrolling to specific homework")

        viewModel.selectedHomeworkId.value?.let {
            val index = homeworkList.getIndexById(it)
            if (index >= 0) {
                viewModel.selectedHomeworkId.value = null
                binding.list.scrollToPosition(index)
            } //else the id if from oder homework list
        }
    }
}