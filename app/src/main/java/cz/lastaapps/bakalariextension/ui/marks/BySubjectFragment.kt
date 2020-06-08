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
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.databinding.FragmentMarksSubjectBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**Shows subjects and their marks
 * placed in MarksRootFragment*/
class BySubjectFragment : Fragment() {
    companion object {
        private val TAG = BySubjectFragment::class.java.simpleName
    }

    //views
    lateinit var binding: FragmentMarksSubjectBinding

    //ViewModel with views
    lateinit var viewModel: MarksViewModel

    //updates fragment on marks update
    private val marksObserver = { _: MarksAllSubjects ->
        Log.i(TAG, "Updating based on new marks")
        loadSubjects()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //obtains viewModel with marks
        val viewModel: MarksViewModel by requireActivity().viewModels()
        this.viewModel = viewModel

        //observes for marks update
        viewModel.marks.observe({ lifecycle }, marksObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //inflates views
        if (!this::binding.isInitialized) {
            Log.i(TAG, "Creating view")

            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_marks_subject, container, false)
            binding.viewmodel = viewModel
            binding.lifecycleOwner = LifecycleOwner { lifecycle }

            //fills up list
            loadSubjects()
        } else {
            Log.i(TAG, "View already created")
        }

        return binding.root
    }

    /**fills up list with subjects*/
    private fun loadSubjects() {
        val loading: ProgressBar = binding.loading
        val errorMessage = binding.errorMessage
        val subjectListView = binding.subjectList

        //loading state
        loading.visibility = View.VISIBLE
        subjectListView.visibility = View.GONE
        errorMessage.visibility = View.GONE

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {

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

            //inits list
            subjectListView.setHasFixedSize(true)
            subjectListView.layoutManager = LinearLayoutManager(subjectListView.context)
            subjectListView.adapter = SubjectAdapter(marks)

            //prevents lags
            yield()

            //shows views
            subjectListView.visibility = View.VISIBLE
            loading.visibility = View.GONE
        }

    }
}