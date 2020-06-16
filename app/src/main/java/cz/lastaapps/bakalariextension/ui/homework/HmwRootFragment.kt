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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.HomeworkStorage
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.databinding.FragmentHomeworkRootBinding

/**Contains all oder homework related homework*/
class HmwRootFragment : Fragment() {

    companion object {
        private val TAG = HmwRootFragment::class.simpleName
        const val navigateToHomeworkId = "homeworkId"
    }

    lateinit var binding: FragmentHomeworkRootBinding
    val viewModel: HmwViewModel by activityViewModels()

    /**updates data when homework list is loaded*/
    private val homeworkObserver = { _: HomeworkList ->
        Log.i(TAG, "Updating based on new data")

        onHomeworkValid()
        setupViewPager()
        lastUpdated()
    }

    /**observes when no data can be obtained (not empty list)*/
    private val failObserver = { failed: Boolean ->
        if (failed) {
            onHomeworkFail()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //observes for homework list update
        viewModel.homework.observe({ lifecycle }, homeworkObserver)
        viewModel.failed.observe({ lifecycle }, failObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //inflates views only once
        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating view")
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_homework_root, container, false)
            binding.lifecycleOwner = LifecycleOwner { lifecycle }
        } else {
            Log.i(TAG, "Already created")
        }

        //updates if the homework list is loaded or starts loading
        if (viewModel.homework.value == null) {
            viewModel.onRefresh(false)
        } else {
            homeworkObserver(viewModel.homework.value!!)
        }

        return binding.root
    }

    /**executed when homework list is loaded*/
    private fun onHomeworkValid() {
        binding.apply {
            loading.visibility = View.GONE
            errorMessage.visibility = View.GONE
            pager.visibility = View.VISIBLE
        }

        //if there is request to show specific homework, shows it
        arguments?.let {
            it.getString(navigateToHomeworkId)?.let {
                selectHomework(it)
            }
        }
    }

    /**shows error info when no homework can be show*/
    private fun onHomeworkFail() {
        binding.apply {
            loading.visibility = View.GONE
            errorMessage.visibility = View.VISIBLE
            pager.visibility = View.GONE

            errorMessage.text = getString(R.string.homework_failed_to_load)
        }
    }

    /**sets up pager with fragments*/
    private fun setupViewPager() {
        binding.apply {
            //sets up ViewPager for oder fragments
            if (pager.adapter == null) {
                pager.adapter = HmwPager(requireContext(), childFragmentManager)
                tabs.setupWithViewPager(pager)
            }
        }
    }

    /**shows homework with id given*/
    private fun selectHomework(id: String) {

        viewModel.selectedHomeworkId.value = id

        val currentHomework = Homework.getCurrent(viewModel.homework.value!!)

        //determinants in which fragment homework is show
        binding.pager.setCurrentItem(
            if (currentHomework.getById(id) != null) {
                0
            } else {
                1
            }, true
        )
    }

    /** shows text at the bottom with time since the last update*/
    private fun lastUpdated() {

        var text = getString(R.string.homework_failed_to_load)
        val lastUpdated = HomeworkStorage.lastUpdated()
        lastUpdated?.let {
            text = cz.lastaapps.bakalariextension.tools.lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}