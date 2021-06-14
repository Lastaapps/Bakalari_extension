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

package cz.lastaapps.bakalari.app.ui.start.version

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import cz.lastaapps.bakalari.app.R

class VersionUpdatedFragment : Fragment() {

    //disables the navigation back option on this fragment
    private val backButtonDisabler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {}
    }

    private val viewModel: VersionViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback({ lifecycle }, backButtonDisabler)

        viewModel.updateVersion()

        viewModel.process.observe({ lifecycle }) {
            when (it) {
                VersionProgressState.STATE_READY -> {
                }
                VersionProgressState.STATE_RUNNING -> {
                }
                VersionProgressState.STATE_SUCCESS -> {
                    findNavController().navigateUp()
                    Toast.makeText(
                        requireContext(),
                        R.string.version_update_success,
                        Toast.LENGTH_LONG
                    ).show()
                }
                VersionProgressState.STATE_FAILED -> {
                    findNavController().navigateUp()
                    Toast.makeText(
                        requireContext(),
                        R.string.version_update_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_version_updater, container, false)

}