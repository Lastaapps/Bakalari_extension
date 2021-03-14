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

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.NavGraphRootDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.start.login.LoginFragmentViewModel.Companion.PROGRESS_DONE
import cz.lastaapps.bakalari.app.ui.start.login.LoginFragmentViewModel.Companion.PROGRESS_ERROR
import cz.lastaapps.bakalari.app.ui.start.login.LoginFragmentViewModel.Companion.PROGRESS_ZERO
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.*

class DownloadDefaultDataDialog : BottomSheetDialogFragment() {

    init {
        isCancelable = false
    }

    private lateinit var root: View
    private val viewModel: LoginViewModel by loginViewModels()
    private val args: DownloadDefaultDataDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = createDialog(requireContext(), container)
            .setDialogTitle(R.string.loading_download_default_title)
            .setDialogMessage(R.string.loading_download_default_message)
            .setPositiveButton(R.string.loading_download_default_positive) {
                root.enableProgressBar()
                viewModel.downloadDefault(args.uuid)
            }
            .setNegativeButton(R.string.loading_download_default_negative) {
                navHome()
            }

        root.getProgressBar().max = 100
        viewModel.defaultDownloadProgress.observe({ lifecycle }) {
            progressChanged(it)
        }
        return root
    }

    private fun progressChanged(progress: Int) {
        root.enableProgressBar(
            progress !in listOf(
                PROGRESS_ZERO,
                PROGRESS_ERROR
            )
        )
        root.showButtons(progress == PROGRESS_ZERO)

        when (progress) {
            PROGRESS_ZERO -> {
                root.getProgressBar().progress = 0
            }
            PROGRESS_ERROR -> {
                Toast.makeText(
                    requireContext(),
                    R.string.loading_download_default_error,
                    Toast.LENGTH_SHORT
                ).show()
                navHome()
            }
            PROGRESS_DONE -> {
                root.getProgressBar().progress = 100

                Toast.makeText(
                    requireContext(),
                    R.string.loading_download_default_done,
                    Toast.LENGTH_SHORT
                ).show()
                navHome()
            }
            else -> {
                if (Build.VERSION.SDK_INT >= 24)
                    root.getProgressBar().setProgress(progress, true)
                else
                    root.getProgressBar().progress = progress
            }
        }
    }

    private fun navHome() {
        findNavController().apply {
            navigate(NavGraphRootDirections.actionLoading(args.uuid, false))
        }
    }
}
