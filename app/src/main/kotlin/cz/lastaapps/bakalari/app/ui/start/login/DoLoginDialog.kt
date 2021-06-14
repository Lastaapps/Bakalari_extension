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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cz.lastaapps.bakalari.app.NavGraphRootDirections
import cz.lastaapps.bakalari.app.R

class DoLoginDialog : DialogFragment() {

    val args: DoLoginDialogArgs by navArgs()
    val viewModel: LoginViewModel by loginViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.login_connecting)
            .setOnCancelListener {
                viewModel.cancelLogin(args.loginInfo.uuid)
            }
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.doLogIn(args.loginInfo, args.profile, args.editedAccount)

        viewModel.loginResult.observe({ lifecycle }) {
            if (it == LoginResult.UNSPECIFIED)
                return@observe

            var toastTextId: Int? = null
            val uuid = args.loginInfo.uuid
            when (it) {
                LoginResult.UPDATED_SIMPLE -> {
                    toastTextId = R.string.login_updated
                    findNavController().navigate(NavGraphRootDirections.actionLoading(uuid, false))
                }
                LoginResult.UPDATED_IMPORTANT -> {
                    toastTextId = R.string.login_updated
                    findNavController().navigate(
                        DoLoginDialogDirections.actionDoLoginToDownloadDefault(
                            uuid
                        )
                    )
                }
                LoginResult.SUCCESS -> {
                    toastTextId = R.string.login_succeeded

                    findNavController().navigate(
                        DoLoginDialogDirections.actionDoLoginToDownloadDefault(
                            uuid
                        )
                    )
                }
                LoginResult.FAIL_WRONG_LOGIN -> {
                    toastTextId = R.string.login_error_wrong_input
                    findNavController().navigateUp()
                }
                LoginResult.FAIL_INTERNET -> {
                    toastTextId = R.string.login_error_server_unavailable
                    findNavController().navigateUp()
                }
                LoginResult.FAIL_INTERNAL_APP_LOGIN -> {
                    toastTextId = R.string.login_error_login_internal
                    findNavController().navigateUp()
                }
            }
            toastTextId?.let { res ->
                Toast.makeText(requireContext(), res, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.loginCancelable.observe({ lifecycle }) {
            dialog?.setCancelable(it)
        }
    }
}