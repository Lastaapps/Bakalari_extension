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
import com.google.android.material.tabs.TabLayoutMediator
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.marks.MarksStorage
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

    private val failObserver = { failed: Boolean ->
        if (failed)
            onFail()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //observes for marks change
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

            binding.pager.isSaveEnabled = false
        } else {
            Log.i(TAG, "Already created")
        }

        viewModel.executeOrRefresh(lifecycle) { marksUpdated() }

        return binding.root
    }

    private fun marksUpdated() {

        Log.i(TAG, "Updating based on mew marks")

        updateLastUpdated()
        loadMarks()
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
                MarksPager(this@MarksRootFragment).also {
                    pager.adapter = it

                    TabLayoutMediator(binding.tabs, pager) { tab, position ->

                        tab.text = it.getPageTitle(position)
                    }.attach()
                }
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

    private fun updateLastUpdated() {

        var text = getString(R.string.marks_failed_to_load)
        val lastUpdated = MarksStorage.lastUpdated()
        lastUpdated?.let {
            text = lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}