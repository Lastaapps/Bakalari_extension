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

package cz.lastaapps.bakalari.app.ui.start.loading

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R

/**Checks if app has been ever started -> license, if user is logged in -> LoginActivity or -> MainActivity*/
class LoadingFragment : Fragment() {

    companion object {
        private val TAG = LoadingFragment::class.java.simpleName
    }

    private val viewModel: LoadingViewModel by activityViewModels()
    private val args: LoadingFragmentArgs by navArgs()

    private var wasAppBarShown = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //hides the app bar and saves it's state
        (requireActivity() as MainActivity).actionBar?.let {
            wasAppBarShown = it.isShowing
            it.hide()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //restores the app bar
        (requireActivity() as MainActivity).actionBar?.let {
            if (wasAppBarShown)
                it.show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Log.i(TAG, "Activity created")

        viewModel.navigateAction.observe({ lifecycle }) {
            it?.let {
                findNavController().apply {
                    popBackStack(R.id.nav_loading, false)
                    navigate(it)
                    viewModel.navigateAction.value = null
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Fragment resumed")

        viewModel.determinateNavigation(args.uuid, args.autoLaunch)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }
}
