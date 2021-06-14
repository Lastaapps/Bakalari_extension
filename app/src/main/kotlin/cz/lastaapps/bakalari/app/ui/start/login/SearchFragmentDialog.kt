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

package cz.lastaapps.bakalari.app.ui.start.login

import android.app.Dialog
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentLoginSearchBinding
import cz.lastaapps.bakalari.tools.searchNeutralText
import cz.lastaapps.bakalari.tools.ui.BasicRecyclerAdapter
import java.text.Collator
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


/**Searches through the town or school list*/
class SearchFragmentDialog : DialogFragment() {

    companion object {
        private val TAG = SearchFragmentDialog::class.java.simpleName
    }

    private val viewModel: LoginFragmentViewModel by loginViewModels()
    private lateinit var binding: FragmentLoginSearchBinding
    private val args: SearchFragmentDialogArgs by navArgs()

    /**If towns or schools should be shown*/
    private var isTown: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        Log.i(TAG, "Creating dialog")

        binding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.fragment_login_search,
            null,
            false
        )
        binding.setLifecycleOwner { lifecycle }

        isTown = args.isTown

        binding.label.text = getString(
            if (isTown) {
                R.string.login_search_select_town
            } else {
                R.string.login_search_select_school
            }
        )

        binding.search.completionHint = getString(
            if (isTown) {
                R.string.login_search_select_town_hint
            } else {
                R.string.login_search_select_school_hint
            }
        )

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                //filters based on new search input
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

        //shows keyboard without clicking the EditText by simulating user click
        binding.search.also {
            it.requestFocus()
            it.postDelayed({
                it.dispatchTouchEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN,
                        0f,
                        0f,
                        0
                    )
                );
                it.dispatchTouchEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP,
                        0f,
                        0f,
                        0
                    )
                )
            }, 200)
        }

        return AlertDialog.Builder(requireContext())
            .setCancelable(true)
            .setView(binding.root)
            .create()
    }

    /**puts data into place*/
    private fun updateData() {
        val filterRaw = binding.search.text.toString()

        /**Obtains the correct list*/
        val dataList =
            if (isTown) {
                viewModel.townList.value!!
            } else {
                viewModel.getSchoolList(viewModel.selectedTown.value!!).value!!
            }

        //extracts names of the objects
        val stringList = ArrayList<Any>()
        val filterNeutral = filterRaw.searchNeutralText()

        if (filterNeutral != "") {
            //filters data
            for (any in dataList) {
                val name = any.toString()
                if (name.searchNeutralText().contains(filterNeutral))
                    stringList.add(any)
            }

            val collator = Collator.getInstance(Locale.getDefault())
            val comparator = Comparator<Any> { o1, o2 ->
                val s1 = o1.toString()
                val s2 = o2.toString()
                val n1 = s1.searchNeutralText()
                val n2 = s2.searchNeutralText()

                when {
                    n1.startsWith(filterNeutral) && !n2.startsWith(filterNeutral) -> -1
                    !n1.startsWith(filterNeutral) && n2.startsWith(filterNeutral) -> +1
                    else -> collator.compare(s1, s2)
                }
            }

            stringList.sortWith(comparator)
        } else {
            stringList.addAll(dataList)
        }

        (binding.list.adapter as BasicRecyclerAdapter<Any>).update(stringList)

        binding.emptyMessage.visibility = if (dataList.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    /**when item is selected*/
    private fun itemClicked(clicked: Any) {
        if (isTown) {

            viewModel.selectedTown.value = clicked as Town
        } else {

            viewModel.selectedSchool.value = clicked as School
        }

        dismiss()
    }
}