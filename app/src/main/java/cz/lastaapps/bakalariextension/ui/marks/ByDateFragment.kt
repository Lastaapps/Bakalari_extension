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
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.databinding.FragmentMarksDateBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**Shows marks from all subjects sorted by date
 * placed in MarksRootFragment*/
class ByDateFragment : Fragment() {

    //root view
    lateinit var binding: FragmentMarksDateBinding
    //ViewModel with marks
    lateinit var viewModel: MarksViewModel

    //executed on marks update
    private val marksObserver = {_: MarksAllSubjects ->
        loadMarks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //obtains ViewModel with marks
        val viewModel: MarksViewModel by requireActivity().viewModels()
        this.viewModel = viewModel

        //observes for marks update
        viewModel.marks.observe({lifecycle}, marksObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        //stops observing for marks update
        viewModel.marks.removeObserver(marksObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //inflates view
        if (!this::binding.isInitialized) {
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_marks_date, container, false)
            binding.viewmodel = viewModel
            binding.lifecycleOwner = LifecycleOwner { lifecycle }

            loadMarks()
        }

        return binding.root
    }

    /**puts marks into view*/
    private fun loadMarks() {
        val loading: ProgressBar = binding.loading
        val errorMessage = binding.errorMessage
        val marksListView = binding.marksList

        //hides views
        loading.visibility = View.VISIBLE
        marksListView.visibility = View.GONE
        errorMessage.visibility = View.GONE

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {

            //marks inited in MarksRootFragment
            val marks = viewModel.marks.value!!

            //gets all marks
            val allMarks = marks.getAllMarks()

            //if there are no marks
            if (allMarks.isEmpty()) {
                errorMessage.setText(R.string.marks_no_marks)
                errorMessage.visibility = View.VISIBLE
                loading.visibility = View.GONE
                return@launch
            }

            //inits marks list
            marksListView.setHasFixedSize(true)
            marksListView.layoutManager = LinearLayoutManager(marksListView.context)
            marksListView.adapter = MarksAdapter(marks)

            //lag protection
            yield()

            //shows views
            marksListView.visibility = View.VISIBLE
            loading.visibility = View.GONE
        }
    }
}

