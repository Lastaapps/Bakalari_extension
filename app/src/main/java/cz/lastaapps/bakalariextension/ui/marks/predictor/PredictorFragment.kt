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

package cz.lastaapps.bakalariextension.ui.marks.predictor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.databinding.FragmentMarksPredictorBinding
import cz.lastaapps.bakalariextension.ui.marks.MarksAdapter
import cz.lastaapps.bakalariextension.ui.marks.MarksViewModel


class PredictorFragment : Fragment(), AdapterView.OnItemSelectedListener, View.OnClickListener {

    //layout
    lateinit var binding: FragmentMarksPredictorBinding

    //ViewModel with marks data
    lateinit var viewModel: MarksViewModel

    /**Updates on marks changed*/
    private val marksObserver = { _: MarksAllSubjects ->
        loadSubjects()
        loadMarks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //obtains ViewModel
        val viewModel: MarksViewModel by requireActivity().viewModels()
        this.viewModel = viewModel

        //observes for marks change
        viewModel.marks.observe({ lifecycle }, marksObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        //stops observing marks change
        viewModel.marks.removeObserver(marksObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //inflates views
        if (!this::binding.isInitialized) {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_marks_predictor, container, false
            )
            binding.viewmodel = viewModel
            binding.lifecycleOwner = LifecycleOwner { lifecycle }

            //sets add mark button functionality
            binding.addMark.setOnClickListener(this)

            //prepares lists with marks
            arrayOf(binding.marksList, binding.addedMarksList).forEach {
                it.layoutManager = LinearLayoutManager(requireContext())
                it.setHasFixedSize(true)
            }

            loadSubjects()
            checkValidity()
            loadMarks()
            loadAddedMarks()
            updateAverage()
        }

        return binding.root
    }

    /**called when predicted marks was changed*/
    private fun marksUpdated() {
        loadAddedMarks()
        updateAverage()
    }

    /**updates views based on if subject has valid marks*/
    private fun checkValidity() {
        //problem message
        val text = when {
            //nor marks for subject given
            viewModel.subjectMarks.isEmpty() -> {
                getString(R.string.marks_no_marks)
            }
            //Averages only normal or only point marks together
            Mark.isMixed(viewModel.subjectMarks) -> {
                getString(R.string.marks_mixed)
            }
            //everything fine
            else -> {
                ""
            }
        }
        binding.apply {
            if (text == "") {
                //everything ok
                markBox.visibility = View.VISIBLE
                addMark.isEnabled = true
            } else {
                //problem
                markBox.visibility = View.GONE
                addMark.isEnabled = false
            }
            errorMessage.text = text
        }
    }

    /**Inits subject spinner*/
    private fun loadSubjects() {
        val marks = viewModel.marks.value!!

        //subject names or "No marks"
        val subjectNames = ArrayList<String>()

        //to stop
        if (marks.subjects.isNotEmpty()) {

            //converts subjects to subjects names
            for (subject in marks.subjects) {
                subjectNames.add(subject.subject.name)
            }

            //enables selection
            binding.subjectSpinner.onItemSelectedListener = this
            binding.subjectSpinner.isEnabled = true
            
        } else {
            //special value - viewModel will return empty lists
            viewModel.predictorSelected.value = -1

            subjectNames.add(getString(R.string.marks_no_marks))

            //disables selection
            binding.subjectSpinner.onItemSelectedListener = null
            binding.subjectSpinner.isEnabled = false
        }

        //creates adapter with subject names
        val adapter = ArrayAdapter(
            binding.subjectSpinner.context,
            android.R.layout.simple_dropdown_item_1line,
            subjectNames
        )

        //puts subjects into spinner
        binding.subjectSpinner.adapter = adapter
    }

    /**loads not predicted marks*/
    private fun loadMarks() {
        val marks = viewModel.subjectMarks

        binding.marksList.adapter = MarksAdapter(marks)
    }

    /**Loads predicted marks for subject given*/
    private fun loadAddedMarks() {
        val marks = viewModel.predictorMarks

        //action done on mark click - edit
        val onEdit: ((mark: Mark) -> Unit) = { mark: Mark ->
            val dialog = AddMarkDialog.build(requireActivity(), { editedMark: Mark ->

                //replaces marks
                viewModel.predictorMarks.remove(mark)
                viewModel.predictorMarks.add(editedMark)
                marksUpdated()

            }, mark)
            dialog.show()
        }

        //action performed on delete action
        val onDelete: ((mark: Mark) -> Unit) = {
            viewModel.predictorMarks.remove(it)
            marksUpdated()
        }

        //creates new adapter with predictor marks
        binding.addedMarksList.adapter = PredictMarksAdapter(marks, onEdit, onDelete)
    }

    /**Recalculates average and updates text*/
    private fun updateAverage() {
        val list = DataIdList<Mark>()

        list.addAll(viewModel.predictorMarks)
        list.addAll(viewModel.subjectMarks)

        //calculates real average
        val average = Mark.calculateAverage(list)

        binding.averageText = average
    }

    /**On subject selected*/
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.predictorSelected.value = position

        //updates fragment views
        checkValidity()
        loadMarks()
        marksUpdated()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    /**on add button clicked*/
    override fun onClick(v: View) {

        val marks = viewModel.subjectMarks

        //creates new mark
        val points = Mark.isAllPoints(marks)
        val mark = Mark.default.apply { isPoints = points }

        //opens dialog to edit mark
        val dialog = AddMarkDialog.build(requireActivity(), {

            //called when valid data inputted into dialog and
            viewModel.predictorMarks.add(it)
            marksUpdated()

        }, mark)
        dialog.show()
    }
}