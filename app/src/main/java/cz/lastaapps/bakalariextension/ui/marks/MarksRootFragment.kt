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
import cz.lastaapps.bakalariextension.api.marks.MarksStorage
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.databinding.FragmentMarksRootBinding
import cz.lastaapps.bakalariextension.tools.lastUpdated

/** Fragment placed in MainActivity in HomeFragment
 * contains ViewPaper with oder mark related fragments - by date, by subject and predictor*/
class MarksRootFragment : Fragment() {
    companion object {
        private val TAG = MarksRootFragment::class.java.simpleName
    }

    //root view
    lateinit var binding: FragmentMarksRootBinding

    //data with marks - puts new marks in
    val viewModel: MarksViewModel by activityViewModels()

    /**observers for mark change, updates last updated text*/
    private val marksObserver = { _: MarksAllSubjects? ->

        Log.i(TAG, "Updating based on mew marks")

        var text = getString(R.string.marks_failed_to_load)
        val lastUpdated = MarksStorage.lastUpdated()
        lastUpdated?.let {
            text = lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text

        loadMarks()
    }

    private val failObserver = { failed: Boolean ->
        if (failed)
            onFail()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //observes for marks change
        viewModel.marks.observe({ lifecycle }, marksObserver)
        viewModel.failed.observe({ lifecycle }, failObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating views")
            //inflates views
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_marks_root, container, false)
            binding.viewmodel = viewModel
            binding.lifecycleOwner = LifecycleOwner { lifecycle }
        } else {
            Log.i(TAG, "Already created")
        }

        if (viewModel.marks.value != null) {
            marksObserver(null)
        } else {
            viewModel.onRefresh()
        }

        return binding.root
    }

    /**loads marks to ViewModel*/
    private fun loadMarks() {

        //hides view until marks are loaded
        binding.apply {
            pager.visibility = View.GONE
            errorMessage.visibility = View.GONE
            lastUpdated.visibility = View.GONE

            //error - no marks
            if (viewModel.marks.value!!.subjects.isEmpty()) {
                errorMessage.setText(R.string.marks_no_marks)
                errorMessage.visibility = View.VISIBLE
                return
            }

            pager.visibility = View.VISIBLE
            lastUpdated.visibility = View.VISIBLE

            //sets up ViewPager for oder fragments
            if (pager.adapter == null) {
                pager.adapter = MarksPager(requireContext(), childFragmentManager)
                tabs.setupWithViewPager(pager)
            }
        }
    }

    /**executed when no marks was loaded*/
    private fun onFail() {
        //error message - loading error
        binding.apply {
            pager.visibility = View.GONE
            errorMessage.visibility = View.VISIBLE
            lastUpdated.visibility = View.GONE

            errorMessage.setText(R.string.marks_failed_to_load)
        }
    }
}