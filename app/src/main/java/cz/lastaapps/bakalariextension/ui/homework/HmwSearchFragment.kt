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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.FragmentHomeworkSearchBinding

/**searches in all homework*/
class HmwSearchFragment : Fragment() {

    lateinit var binding: FragmentHomeworkSearchBinding
    lateinit var viewModel: HmwViewModel

    //subject objects list
    private lateinit var subjectList: ArrayList<SimpleData>

    /**On homework list updated*/
    private val homeworkObserver = { _: DataIdList<Homework> ->
        initSubjects()
        spinnerAdapterSetup()
        updateList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //loads homework
        val v: HmwViewModel by requireActivity().viewModels()
        viewModel = v

        //observes for data change
        viewModel.homework.observe({ lifecycle }, homeworkObserver)
    }

    override fun onDestroy() {
        super.onDestroy()

        //stop observing
        viewModel.homework.removeObserver(homeworkObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //init views, called only once
        if (!this::binding.isInitialized) {
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

            binding.list.layoutManager = LinearLayoutManager(requireContext())

            binding.subjectSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        viewModel.subjectIndex.value = position
                        updateList()
                    }
                }
            //restores text
            binding.text.setText(viewModel.searchText.value!!)
            binding.text.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.searchText.value = s.toString()
                    updateList()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            //shows list if there is any data
            viewModel.homework.value?.let {
                homeworkObserver(it)
            }
        }

        return binding.root
    }

    /**loads subjects*/
    private fun initSubjects() {
        val subjects = HashSet<SimpleData>()
        viewModel.homework.value?.forEach {
            subjects.add(it.subject)
        }
        subjectList = ArrayList(subjects.sortedBy { it.name })
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
        binding.subjectSpinner.setSelection(0)
        binding.subjectSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)

        if (viewModel.subjectIndex.value!! >= names.size) {
            viewModel.subjectIndex.value = names.size - 1
        }

        binding.subjectSpinner.setSelection(viewModel.subjectIndex.value!!)
    }

    /**updates view with filtered homework list*/
    private fun updateList() {
        var filtered = viewModel.homework.value!!

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
        var adapter = binding.list.adapter as HmwAdapter?
        if (adapter != null) {
            adapter.list = filtered
            adapter.notifyDataSetChanged()
        } else {
            adapter = HmwAdapter(filtered, requireActivity() as AppCompatActivity)
            binding.list.adapter = adapter
        }
    }

    /**@return id of currently selected subject or null for all subjects*/
    private fun getSubjectId(): String? {
        var position = viewModel.subjectIndex.value!!

        if (position == 0)
            return null

        return subjectList[--position].id
    }
}