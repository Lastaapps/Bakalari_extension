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
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.databinding.FragmentHomeworkOverviewBinding

/**placed inside HomeFragment, shows how many current homework are there*/
class HmwOverview : Fragment() {

    companion object {
        private val TAG = HmwOverview::class.simpleName
    }

    lateinit var binding: FragmentHomeworkOverviewBinding
    lateinit var viewModel: HmwViewModel

    /**observes when marks are loaded*/
    private val homeworkObserver = { _: HomeworkList ->
        Log.i(TAG, "Updating based on new homework list")

        onHomeworkValid()
    }

    /**observes when no homework list could be loaded (not empty one)*/
    private val failObserver = { _: Any ->
        if (viewModel.homework.value == null) {
            onHomeworkFail()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //loads ViewModel
        val v: HmwViewModel by requireActivity().viewModels()
        viewModel = v

        //observes for data change
        viewModel.homework.observe({ lifecycle }, homeworkObserver)
        viewModel.failed.observe({ lifecycle }, failObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //inflates views
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_homework_overview,
                container,
                false
            )
        binding.lifecycleOwner = LifecycleOwner { lifecycle }
        binding.viewmodel = viewModel

        //navigates to homework fragments
        binding.contentLayout.setOnClickListener {
            it.findNavController().navigate(R.id.nav_homework)
        }

        //starts marks loading if they aren't yet
        if (viewModel.homework.value == null) {
            viewModel.onRefresh(false)
        } else {
            //marks loaded
            homeworkObserver(viewModel.homework.value!!)
        }

        return binding.root
    }

    /**homework list loaded*/
    private fun onHomeworkValid() {
        binding.apply {
            loading.visibility = View.GONE
            errorMessage.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
        }

        setupContent()
    }

    /**failed to download homework list*/
    private fun onHomeworkFail() {
        binding.apply {
            loading.visibility = View.GONE
            errorMessage.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE

            errorMessage.text = getString(R.string.homework_failed_to_load)
        }
    }

    /**sets actual content of the fragment*/
    private fun setupContent() {

        val currentHomework = Homework.getCurrent(viewModel.homework.value!!)
        val size = currentHomework.size
        val text = if (size > 0) {
            resources.getQuantityString(R.plurals.homework_current_homework_template, size, size)
        } else {
            getString(R.string.homework_no_homework)
        }

        binding.text.text = text
    }
}