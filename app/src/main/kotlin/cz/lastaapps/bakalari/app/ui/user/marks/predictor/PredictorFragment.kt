/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalari.app.ui.user.marks.predictor

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.entity.marks.*
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentMarksPredictorBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.marks.MarksAdapter
import cz.lastaapps.bakalari.app.ui.user.marks.MarksViewModel

private typealias PairAdapter = ArrayAdapter<PairHolder>

class PredictorFragment : Fragment(), AdapterView.OnItemSelectedListener, View.OnClickListener {
    companion object {
        private val TAG = PredictorFragment::class.java.simpleName
    }

    //layout
    private lateinit var binding: FragmentMarksPredictorBinding

    //ViewModel with marks data
    private val viewModel: MarksViewModel by accountsViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //inflates views
        Log.i(TAG, "Creating view")

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_marks_predictor, container, false
        )
        binding.viewModel = viewModel
        binding.setLifecycleOwner { lifecycle }

        binding.list.adapter = MarksAdapter()

        //sets add mark button functionality
        binding.addMark.setOnClickListener(this)

        viewModel.runOrRefresh(lifecycle) {
            Log.i(TAG, "updating based on new marks")

            if (viewModel.pairSelected.value == null && it.isNotEmpty())
                viewModel.pairSelected.value = it[0]

            loadSubjects(it)
        }

        viewModel.runOrRefresh(viewModel.pairSelected, lifecycle) {
            //updates fragment views
            checkValidity(it)
            loadMarks(it)
            predictorMarksUpdated()
        }

        return binding.root
    }

    /**called when predicted marks was changed*/
    private fun predictorMarksUpdated() {
        loadPredictorMarks()
        updateNewAverage()
    }

    /**updates views based on if subject has valid marks*/
    private fun checkValidity(pair: MarksPair) {
        //problem message
        val text = when {
            //nor marks for subject given
            pair.marks.isEmpty() -> {
                getString(R.string.marks_no_marks)
            }
            //Averages only normal or only point marks together
            Mark.isMixed(pair.marks) -> {
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
    private fun loadSubjects(marks: MarksPairList) {

        //subject names or "No marks"
        val subjectNames = ArrayList<PairHolder>()

        //to stop
        if (marks.subjects().isNotEmpty()) {

            //converts subjects to subjects names
            subjectNames.addAll(marks.map { PairHolder(it) })

            //enables selection
            binding.subjectSpinner.onItemSelectedListener = this
            binding.subjectSpinner.isEnabled = true

        } else {
            //special value - viewModel will return empty lists
            viewModel.predictorSelected.value = -1

            subjectNames.add(PairHolder(getString(R.string.marks_no_marks)))

            //disables selection
            binding.subjectSpinner.onItemSelectedListener = null
            binding.subjectSpinner.isEnabled = false
        }

        //creates adapter with subject names
        val adapter = PairAdapter(
            binding.subjectSpinner.context,
            android.R.layout.simple_dropdown_item_1line,
            subjectNames
        )

        //puts subjects into spinner
        binding.subjectSpinner.adapter = adapter
    }

    /**loads not predicted marks*/
    private fun loadMarks(pair: MarksPair) {
        viewModel.average.value = pair.subject.averageText
        (binding.list.adapter as MarksAdapter).updateMarks(pair.marks)
    }

    /**Loads predicted marks for subject given*/
    private fun loadPredictorMarks() {
        val marks = viewModel.predictorMarks

        //action done on mark click - edit
        val onEdit: ((mark: Mark) -> Unit) = { mark: Mark ->
            val dialog = AddMarkDialog.build(requireActivity(), { editedMark: Mark ->

                //replaces marks
                viewModel.predictorMarks.remove(mark)
                viewModel.predictorMarks.add(editedMark)
                predictorMarksUpdated()

            }, mark)
            dialog.show()
        }

        //action performed on delete action
        val onDelete: ((mark: Mark) -> Unit) = {
            viewModel.predictorMarks.remove(it)
            predictorMarksUpdated()
        }

        //creates new adapter with predictor marks
        binding.addedMarksList.adapter = PredictMarksAdapter(marks, onEdit, onDelete)
    }

    /**Recalculates average and updates text*/
    private fun updateNewAverage() {
        val list = DataIdList<Mark>()

        val marks = viewModel.pairSelected.value?.marks
            ?: MarksList()

        list.addAll(viewModel.predictorMarks)
        list.addAll(marks)

        //calculates real average
        val newAverage = Mark.calculateAverage(list)
        viewModel.newAverage.value = newAverage

        val comparison = viewModel.average.value!!.trim().compareTo(newAverage)
        viewModel.newAverageColor.value =
            when {
                comparison == 0 || marks.isEmpty() || Mark.isMixed(marks) -> {
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            cz.lastaapps.bakalari.core.R.color.primary_text_light
                        )
                    )
                }
                (comparison > 0) == Mark.isAllNormal(marks) -> {
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.holo_green_light
                        )
                    )
                }
                else -> {
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.holo_red_light
                        )
                    )
                }
            }
    }

    /**On subject selected*/
    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        viewModel.predictorSelected.value = position
        viewModel.pairSelected.value = (parent.adapter as PairAdapter).getItem(position)!!.pair
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    /**on add button clicked*/
    override fun onClick(v: View) {

        val pair = viewModel.pairSelected.value

        if (pair == null) {
            Toast.makeText(requireContext(), R.string.marks_no_marks, Toast.LENGTH_SHORT).show()

            return
        }

        val marks = pair.marks

        //creates new mark
        val points = Mark.isAllPoints(marks)
        val isWeightZero = marks[0].weight == null

        var mark = Mark.default.copy(isPoints = points)
        if (isWeightZero)
            mark = mark.copy(weight = null)

        //opens dialog to edit mark
        val dialog = AddMarkDialog.build(requireActivity(), {

            //called when valid data inputted into dialog and
            viewModel.predictorMarks.add(it)
            predictorMarksUpdated()

        }, mark)
        dialog.show()
    }
}

/** Used in ArrayAdapter - implements custom #toString() method to show the right labels*/
private class PairHolder {

    val pair: MarksPair?
    private val customMessage: String

    constructor(pair: MarksPair) {
        this.pair = pair
        this.customMessage = ""
    }

    constructor(message: String) {
        this.pair = null
        this.customMessage = message
    }

    fun hasData() = pair != null

    override fun toString(): String = pair?.subject?.subject?.name ?: customMessage
}