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

package cz.lastaapps.bakalari.app.ui.user.homework

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.TemplateLoadingRootBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**Contains all oder homework related homework*/
class HmwRootFragment : Fragment() {

    companion object {
        private val TAG = HmwRootFragment::class.simpleName
        const val navigateToHomeworkId = "homeworkId"
    }

    lateinit var binding: TemplateLoadingRootBinding
    val viewModel: HmwViewModel by accountsViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.template_loading_root, container, false)
        binding.lifecycleOwner = LifecycleOwner { lifecycle }
        binding.viewModel = viewModel

        //add adapter to pager and sets up the connection with the TabLayout
        HmwPager(this@HmwRootFragment).also {
            binding.pager.adapter = it

            TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->

                tab.text = it.getPageTitle(position)
            }.attach()
        }
        binding.pager.offscreenPageLimit = 2

        //updates if the homework list is loaded or starts loading
        if (viewModel.hasData.value == true) {
            lifecycleScope.launch(Dispatchers.Default) {
                presetHomeworkCheck()
            }
        }

        viewModel.onDataUpdate(lifecycle) {
            dataChanged()
        }

        return binding.root
    }

    /**updates data when homework list is loaded*/
    private fun dataChanged() {
        Log.i(TAG, "Updating based on new data")

        lastUpdated()
    }

    /**executed when homework list is loaded*/
    private suspend fun presetHomeworkCheck() {

        //if there is request to show specific homework, shows it
        arguments?.let {
            it.getString(navigateToHomeworkId)?.let { id ->

                withContext(Dispatchers.Main) {

                    viewModel.selectedHomeworkId.value = id

                    val isCurrentHomework = viewModel.isCurrent(id)

                    //determinants in which fragment homework is show
                    binding.pager.setCurrentItem(
                        if (isCurrentHomework) {
                            0
                        } else {
                            1
                        }, true
                    )
                }
            }
        }
    }

    /** shows text at the bottom with time since the last update*/
    private fun lastUpdated() {

        var text = getString(R.string.homework_failed_to_load)
        viewModel.lastUpdated()?.let {
            text = cz.lastaapps.bakalari.tools.ui.lastUpdated(requireContext(), it)
        }

        binding.lastUpdated.text = text
    }
}