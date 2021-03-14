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

package cz.lastaapps.bakalari.app.ui.start.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.*
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmDeleteDialog : BottomSheetDialogFragment() {

    private val args: ConfirmDeleteDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = createDialog(requireContext(), container)

        val message = getString(R.string.profile_delete_message).format(args.account.profileName)

        root.setDialogTitle(R.string.profile_delete_title)
        root.setDialogMessage(message)
        root.setDialogButton(BUTTON_POSITIVE, R.string.profile_delete_positive) {
            root.isEnabled = false

            lifecycleScope.launch(Dispatchers.Default) {

                AccountsDatabase.getDatabase(requireContext()).repository.removeAccount(
                    requireContext(), args.account
                )

                withContext(Dispatchers.Main) {
                    dismiss()
                }
            }
        }
        root.setDialogButton(BUTTON_NEGATIVE, R.string.profile_delete_negative) {
            dismiss()
        }

        return root
    }

}