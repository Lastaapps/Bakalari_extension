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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.marks.Marks
import cz.lastaapps.bakalariextension.api.marks.MarksStorage
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.tools.lastUpdated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fragment placed in MainActivity in HomeFragment
 * contains ViewPaper with oder mark related fragments - by date, by subject and predictor*/
class MarksRootFragment : Fragment() {

    //root view
    lateinit var root: View

    //data with marks - puts new marks in
    lateinit var viewModel: MarksViewModel

    /**observers for mark change, updates last updated text*/
    private val lastUpdatedObserver = { _: MarksAllSubjects ->

        var text = getString(R.string.marks_failed_to_load)
        val lastUpdated = MarksStorage.lastUpdated()
        lastUpdated?.let {
            text = lastUpdated(requireContext(), it)
        }

        root.findViewById<TextView>(R.id.last_updated).text = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //obtains ViewModel
        val viewModel: MarksViewModel by requireActivity().viewModels()
        this.viewModel = viewModel

        //observes for marks change
        viewModel.marks.observe({ lifecycle }, lastUpdatedObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        //stops observing for marks update
        viewModel.marks.removeObserver(lastUpdatedObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //inflates views
        root = inflater.inflate(R.layout.fragment_marks_root, container, false)

        loadMarks()

        return root
    }

    /**loads marks to ViewModel*/
    private fun loadMarks() {
        val loading = root.findViewById<ProgressBar>(R.id.loading)
        val errorMessage = root.findViewById<TextView>(R.id.error_message)
        val pager = root.findViewById<ViewPager>(R.id.marks_pager)
        val tabs = root.findViewById<TabLayout>(R.id.tabs)
        val lastUpdated = root.findViewById<TextView>(R.id.last_updated)

        //hides view until marks are loaded
        loading.visibility = View.VISIBLE
        pager.visibility = View.GONE
        errorMessage.visibility = View.GONE
        lastUpdated.visibility = View.GONE

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {

            //loasds marks from storage
            val allSubjectMarks = viewModel.marks.value
                ?: Marks.loadFromStorage()

            withContext(Dispatchers.Main) {

                //refreshes marks from server if there ase no marks or they should be refreshed
                if (Marks.shouldReload() || allSubjectMarks == null)
                    viewModel.onRefresh(requireContext())

                loading.visibility = View.GONE

                //error message - loading error
                if (allSubjectMarks == null) {
                    errorMessage.setText(R.string.marks_failed_to_load)
                    errorMessage.visibility = View.VISIBLE
                    return@withContext
                }

                //error - no marks
                if (allSubjectMarks.subjects.isEmpty()) {
                    errorMessage.setText(R.string.marks_no_marks)
                    errorMessage.visibility = View.VISIBLE
                    return@withContext
                }

                //updates marks
                if (viewModel.marks.value == null)
                    viewModel.marks.value = allSubjectMarks

                pager.visibility = View.VISIBLE
                lastUpdated.visibility = View.VISIBLE

                //sets up ViewPager for oder fragments
                pager.adapter = FragmentPager(requireContext(), childFragmentManager)
                tabs.setupWithViewPager(pager)
            }
        }
    }
}