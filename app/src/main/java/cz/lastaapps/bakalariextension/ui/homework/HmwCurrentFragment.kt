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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.FragmentHomeworkCurrentBinding

/** Shows current and not done yet homework list*/
class HmwCurrentFragment : Fragment() {

    lateinit var binding: FragmentHomeworkCurrentBinding
    lateinit var viewModel: HmwViewModel

    /**observers for homework list change*/
    var homeworkObserver = { _: DataIdList<Homework> ->
        updateHomework()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //loads ViewModel
        val v: HmwViewModel by requireActivity().viewModels()
        viewModel = v

        //observers for homework list change
        viewModel.homework.observe({ lifecycle }, homeworkObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        //stops observing
        viewModel.homework.removeObserver(homeworkObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //inflates view
        if (!this::binding.isInitialized) {
            binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_homework_current,
                container,
                false
            )
            //setup
            binding.apply {
                lifecycleOwner = LifecycleOwner { lifecycle }
                viewmodel = viewModel

                list.layoutManager = LinearLayoutManager(list.context)
            }

            updateHomework()
        }

        return binding.root
    }

    /**shows homework list*/
    private fun updateHomework() {

        //filters homework
        val currentHomework = Homework.getCurrent(viewModel.homework.value!!)

        binding.apply {

            loading.visibility = View.GONE

            if (currentHomework.size > 0) {
                list.adapter = HmwAdapter(currentHomework, requireActivity() as AppCompatActivity)
                errorMessage.visibility = View.GONE

                scrollToHomework(currentHomework)
            } else {
                //no homework loaded, but set up data anyway
                list.adapter = HmwAdapter(DataIdList(), requireActivity() as AppCompatActivity)
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = getString(R.string.homework_no_current)
            }
        }
    }

    /**If there was request to show specific homework, scrolls there*/
    private fun scrollToHomework(homeworkList: DataIdList<Homework>) {
        viewModel.selectedHomeworkId.value?.let {
            val index = homeworkList.getIndexById(it)
            if (index >= 0) {
                viewModel.selectedHomeworkId.value = null
                binding.list.scrollToPosition(index)
            } //else the id if from oder homework list
        }
    }
}