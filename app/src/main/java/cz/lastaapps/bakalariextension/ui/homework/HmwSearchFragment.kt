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
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.FragmentHomeworkSearchBinding
import cz.lastaapps.bakalariextension.ui.EmptyAdapter

/**searches in all homework*/
class HmwSearchFragment : Fragment() {

    companion object {
        private val TAG = HmwSearchFragment::class.java.simpleName
    }

    lateinit var binding: FragmentHomeworkSearchBinding
    val viewModel: HmwViewModel by activityViewModels()

    //subject objects list
    private lateinit var subjectList: ArrayList<SimpleData>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating fragment view")

        //inflates views
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_homework_search,
            container,
            false
        )
        //init
        binding.viewmodel = viewModel
        binding.setLifecycleOwner { lifecycle }

        binding.subjectSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            ArrayList<String>()
        )

        binding.list.adapter = EmptyAdapter(HmwAdapter(requireActivity() as AppCompatActivity))

        //TODO one button and animations + date filter
        //switches between search methods - subjects spinner and text
        binding.searchSelectButton.setOnClickListener {
            viewModel.searchingUsingSpinner.value = viewModel.searchingUsingSpinner.value != true
        }

        binding.searchTextButton.setOnClickListener {
            viewModel.searchingUsingSpinner.value = viewModel.searchingUsingSpinner.value != true
        }

        viewModel.searchingUsingSpinner.observe({ lifecycle }) {
            binding.apply {
                if (it) {
                    searchSelectButton.visibility = View.INVISIBLE
                    searchTextButton.visibility = View.VISIBLE
                    textSearch.visibility = View.INVISIBLE
                    subjectSpinner.visibility = View.VISIBLE
                } else {
                    searchSelectButton.visibility = View.VISIBLE
                    searchTextButton.visibility = View.INVISIBLE
                    textSearch.visibility = View.VISIBLE
                    subjectSpinner.visibility = View.INVISIBLE
                }
            }
        }

        //shows list if there is any data
        viewModel.executeOrRefresh(lifecycle) { dataUpdated() }

        viewModel.searchText.observe({ lifecycle }) { updateList() }
        viewModel.subjectIndex.observe({ lifecycle }) { updateList() }

        return binding.root
    }

    /**loads subjects*/
    private fun initSubjects() {
        val subjects = HashSet<SimpleData>()
        viewModel.homework.value?.forEach {
            subjects.add(it.subject)
        }
        subjectList = ArrayList(subjects.sorted())
    }

    /**sets up spinner with All subject option and subject names*/
    private fun spinnerAdapterSetup() {
        val names = ArrayList<String>()

        //the opinion to don't filter by subject
        names.add(getString(R.string.homework_search_all))

        //adds subject names
        subjectList.forEach {
            names.add(it.name)
        }

        //reselect same item as before if possible
        (binding.subjectSpinner.adapter as? ArrayAdapter<String>)?.also {
            it.clear()
            it.addAll(names)
            it.notifyDataSetChanged()
        }

        //protection against if list gets shorter and index is now greater then new list size
        if (viewModel.subjectIndex.value!! >= names.size) {
            viewModel.subjectIndex.value = names.lastIndex
        }
    }

    /**On homework list updated*/
    private fun dataUpdated() {
        Log.i(TAG, "Updating with new homework list")

        initSubjects()
        spinnerAdapterSetup()
        updateList()
    }

    /**updates view with filtered homework list*/
    private fun updateList() {
        var filtered = viewModel.homework.value ?: return

        //filters by subject
        getSubjectId()?.let {
            filtered = Homework.getBySubject(filtered, it)
        }

        //filters by text
        val text = viewModel.searchText.value
        if (text != null && text != "") {
            filtered = Homework.getByText(filtered, text)
        }

        //sets up view with adapter or updates current adapter
        EmptyAdapter.getAdapter<HmwAdapter>(binding.list).update(filtered)
    }

    /**@return id of currently selected subject or null for all subjects*/
    private fun getSubjectId(): String? {
        var position = viewModel.subjectIndex.value!!

        if (position == 0)
            return null

        return subjectList[--position].id
    }
}