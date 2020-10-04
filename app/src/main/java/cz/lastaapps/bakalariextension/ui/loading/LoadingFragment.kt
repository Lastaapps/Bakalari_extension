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

package cz.lastaapps.bakalariextension.ui.loading

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.MainViewModel
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.license.LicenseFragment

/**Checks if app has been ever started -> license, if user is logged in -> LoginActivity or -> MainActivity*/
class LoadingFragment : Fragment() {

    companion object {
        private val TAG = LoadingFragment::class.java.simpleName
    }

    private val mainViewModel: MainViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Log.i(TAG, "Activity created")

        mainViewModel.apply {
            val doWork = { state: Int ->
                when (state) {
                    MainViewModel.LOGGED_IN -> {
                        userViewModel.runOrRefresh(lifecycle) {
                            (requireActivity() as MainActivity).loginCheckDone()
                        }
                    }
                    MainViewModel.SHOW_LICENSE -> {
                        //shows license dialog
                        LicenseFragment.showDialog(requireActivity(), Runnable {
                            mainViewModel.reset()
                            mainViewModel.doDecision()
                        })
                    }
                    MainViewModel.SHOW_NO_INTERNET -> {

                        //no internet, no log in
                        Toast.makeText(
                            requireContext(),
                            R.string.loading_no_internet,
                            Toast.LENGTH_LONG
                        ).show()

                        AlertDialog.Builder(requireContext())
                            .setMessage(R.string.loading_no_saved_data)
                            .setCancelable(false)
                            .setPositiveButton(R.string.close) { _: DialogInterface, _: Int ->
                                requireActivity().finish()
                            }
                            .setNegativeButton(R.string.retry) { _: DialogInterface, _: Int ->
                                mainViewModel.reset()
                                mainViewModel.doDecision()
                            }
                            .create()
                            .show()
                    }
                    MainViewModel.SHOW_LOGIN_ACTIVITY -> {
                        requireActivity().findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.nav_login)
                    }
                }
            }

            result.observe({ lifecycle }, doWork)

            if (result.value == MainViewModel.UNKNOWN) {
                doDecision()
            }
        }
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
