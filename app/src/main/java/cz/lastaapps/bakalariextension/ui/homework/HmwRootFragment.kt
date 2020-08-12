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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.tabs.TabLayoutMediator
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.HomeworkStorage
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.TemplateLoadingRootBinding

/**Contains all oder homework related homework*/
class HmwRootFragment : Fragment() {

    companion object {
        private val TAG = HmwRootFragment::class.simpleName
        const val navigateToHomeworkId = "homeworkId"
    }

    lateinit var binding: TemplateLoadingRootBinding
    val viewModel: HmwViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.template_loading_root, container, false)
        binding.lifecycleOwner = LifecycleOwner { lifecycle }
        binding.viewmodel = viewModel

        //add adapter to pager and sets up the connection with the TabLayout
        HmwPager(this@HmwRootFragment).also {
            binding.pager.adapter = it

            TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->

                tab.text = it.getPageTitle(position)
            }.attach()
        }
        binding.pager.offscreenPageLimit = 2

        //updates if the homework list is loaded or starts loading
        viewModel.executeOrRefresh(lifecycle) { dataChanged() }

        return binding.root
    }

    /**updates data when homework list is loaded*/
    private fun dataChanged() {
        Log.i(TAG, "Updating based on new data")

        onHomeworkValid()
        //setupViewPager()
        lastUpdated()
    }

    /**executed when homework list is loaded*/
    private fun onHomeworkValid() {

        //if there is request to show specific homework, shows it
        arguments?.let {
            it.getString(navigateToHomeworkId)?.let {
                selectHomework(it)
            }
        }
    }

    /**sets up pager with fragments*/
    private fun setupViewPager() {
        binding.apply {
            //sets up ViewPager for oder fragments
            if (pager.adapter == null) {
                HmwPager(this@HmwRootFragment).also {
                    pager.adapter = it

                    TabLayoutMediator(binding.tabs, pager) { tab, position ->

                        tab.text = it.getPageTitle(position)
                    }.attach()
                }
            }
        }
    }

    /**shows homework with id given*/
    private fun selectHomework(id: String) {

        viewModel.selectedHomeworkId.value = id

        val currentHomework = Homework.getCurrent(viewModel.homework.value!!)

        //determinants in which fragment homework is show
        binding.pager.setCurrentItem(
            if (currentHomework.getById(id) != null) {
                0
            } else {
                1
            }, true
        )
    }

    /** shows text at the bottom with time since the last update*/
    private fun lastUpdated() {

        var text = getString(R.string.homework_failed_to_load)
        val lastUpdated = HomeworkStorage.lastUpdated()
        lastUpdated?.let {
            text = cz.lastaapps.bakalariextension.tools.lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}