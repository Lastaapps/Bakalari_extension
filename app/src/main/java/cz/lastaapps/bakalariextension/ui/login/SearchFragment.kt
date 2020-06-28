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

package cz.lastaapps.bakalariextension.ui.login

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.FragmentLoginSearchBinding
import cz.lastaapps.bakalariextension.tools.searchNeutralText
import cz.lastaapps.bakalariextension.ui.BasicRecyclerAdapter
import cz.lastaapps.bakalariextension.ui.login.LoginFragment.School
import cz.lastaapps.bakalariextension.ui.login.LoginFragment.Town

class SearchFragment : DialogFragment() {

    companion object {
        private val TAG = SearchFragment::class.java.simpleName
        private const val IS_TOWN_EXTRA = "IS_TOWN"

        fun initialize(isTown: Boolean): SearchFragment {
            val args = Bundle().apply {
                putBoolean(IS_TOWN_EXTRA, isTown)
            }

            return SearchFragment().also {
                it.arguments = args
            }
        }
    }

    private val viewModel: LoginViewModel by activityViewModels()
    private lateinit var binding: FragmentLoginSearchBinding

    private var isTown: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        binding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.fragment_login_search,
            null,
            false
        )
        binding.setLifecycleOwner { lifecycle }

        isTown = requireArguments().getBoolean(IS_TOWN_EXTRA)

        binding.label.text = getString(
            if (isTown) {
                R.string.login_search_select_town
            } else {
                R.string.login_search_select_school
            }
        )

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateData()
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

        binding.list.adapter = BasicRecyclerAdapter<Any>({ it.toString() }).apply {
            onItemClicked = { itemClicked(it) }
        }

        updateData()

        return AlertDialog.Builder(requireContext())
            .setCancelable(true)
            .setView(binding.root)
            .create()
    }

    private fun updateData() {
        val filter = binding.search.text.toString()

        val dataList =
            if (isTown) {
                viewModel.townList.value!!
            } else {
                viewModel.getSchoolList(viewModel.selectedTown.value!!).value!!
            }

        val stringList = ArrayList<Any>()

        for (any in dataList) {
            val name = any.toString()
            if (searchNeutralText(name).contains(searchNeutralText(filter)))
                stringList.add(any)
        }

        (binding.list.adapter as BasicRecyclerAdapter<Any>).update(stringList)

        binding.emptyMessage.visibility = if (dataList.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun itemClicked(clicked: Any) {
        if (isTown) {

            viewModel.selectedTown.value = clicked as Town
        } else {

            viewModel.selectedSchool.value = clicked as School
        }

        dismiss()
    }
}