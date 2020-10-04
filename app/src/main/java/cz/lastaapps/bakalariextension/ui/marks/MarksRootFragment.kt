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
import com.google.android.material.tabs.TabLayoutMediator
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.TemplateLoadingRootBinding
import cz.lastaapps.bakalariextension.tools.lastUpdated

/** Fragment placed in MainActivity in HomeFragment
 * contains ViewPaper with oder mark related fragments - by date, by subject and predictor*/
class MarksRootFragment : Fragment() {
    companion object {
        private val TAG = MarksRootFragment::class.java.simpleName
    }

    //root view
    lateinit var binding: TemplateLoadingRootBinding

    //data with marks - puts new marks in
    val viewModel: MarksViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating views")
        //inflates views
        binding =
            DataBindingUtil.inflate(inflater, R.layout.template_loading_root, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = LifecycleOwner { lifecycle }

        //sets up ViewPager for oder fragments
        MarksPager(this).also {
            binding.pager.adapter = it

            TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->

                tab.text = it.getPageTitle(position)
            }.attach()
        }
        binding.pager.offscreenPageLimit = 2

        viewModel.onDataUpdate(lifecycle) { marksUpdated() }

        return binding.root
    }

    private fun marksUpdated() {

        Log.i(TAG, "Updating based on mew marks")

        updateLastUpdated()
    }

    private fun updateLastUpdated() {

        var text = viewModel.failedText(requireContext())
        val lastUpdated = viewModel.lastUpdated()
        lastUpdated?.let {
            text = lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}