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
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksRoot
import cz.lastaapps.bakalariextension.databinding.FragmentMarksNewBinding

/**Fragment shown in HomeFragment
 * shown new marks*/
class NewMarksFragment : Fragment() {
    companion object {
        private val TAG = NewMarksFragment::class.java.simpleName
    }

    //views
    lateinit var binding: FragmentMarksNewBinding

    //data - marks
    val viewModel: MarksViewModel by activityViewModels()

    //updates hen new marks are downloaded
    private val marksObserver = { _: MarksRoot? ->
        Log.i(TAG, "Updating with new marks")
        loadMarks()
    }

    private val failObserver = { _: Any ->
        onFail()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //observes for marks update
        viewModel.marks.observe({ lifecycle }, marksObserver)
        viewModel.failed.observe({ lifecycle }, failObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //creates view
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_marks_new, container, false)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = LifecycleOwner { lifecycle }

        if (viewModel.marks.value != null) {
            marksObserver(null)
        } else {
            viewModel.onRefresh()
        }

        return binding.root
    }

    /**Loads marks*/
    private fun loadMarks() {
        val marks = viewModel.marks.value!!
        val allMarks = marks.getAllMarks()
        val newMarks = DataIdList<Mark>()

        //filters new marks
        for (mark in allMarks)
            if (mark.showAsNew())
                newMarks.add(mark)

        //is hidden when on no marks
        if (newMarks.isEmpty()) {
            return
        }

        //puts views in
        val marksListView = binding.list
        marksListView.setHasFixedSize(true)
        marksListView.adapter = MarksAdapter(marks, newMarks)
    }

    /**When marks failed to load*/
    private fun onFail() {
        binding.list.adapter = null
    }
}