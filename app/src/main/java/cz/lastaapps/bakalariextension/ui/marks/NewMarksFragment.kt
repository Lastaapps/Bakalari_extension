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
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.MarksLoader
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.databinding.FragmentMarksNewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**Fragment shown in HomeFragment
 * shown new marks*/
class NewMarksFragment : Fragment() {

    //views
    lateinit var binding: FragmentMarksNewBinding
    //data - marks
    lateinit var viewModel: MarksViewModel

    //updates hen new marks are downloaded
    private val marksObserver = { _: MarksAllSubjects ->
        loadMarks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //init view model
        val viewModel: MarksViewModel by requireActivity().viewModels()
        this.viewModel = viewModel

        //observes for marks update
        viewModel.marks.observe({lifecycle}, marksObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        //stops observing
        viewModel.marks.removeObserver(marksObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //creates view
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_marks_new, container, false)

        //inits
        loadMarks()

        return binding.root
    }

    /**Loads marks*/
    private fun loadMarks() {
        CoroutineScope(Dispatchers.IO).launch {

            //loads marks
            val marks = viewModel.marks.value
                ?: MarksLoader.loadMarks() ?: return@launch

            withContext(Dispatchers.Main) {
                //puts marks into viewModel
                if (viewModel.marks.value == null)
                    viewModel.marks.value = marks

                val allMarks = marks.getAllMarks().reversed()
                val newMarks = DataIdList<Mark>()

                //filters new marks
                for (mark in allMarks)
                    if (mark.showAsNew())
                        newMarks.add(mark)

                //is hidden when on no marks
                if (newMarks.isEmpty()) {
                    return@withContext
                }

                //puts views in
                val marksListView = binding.marksList
                marksListView.setHasFixedSize(true)
                marksListView.layoutManager = LinearLayoutManager(marksListView.context)
                marksListView.adapter = MarksAdapter(marks, newMarks)
            }
        }
    }
}