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

package cz.lastaapps.bakalari.app.ui.start.login.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.createDialog
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setDialogMessage
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setDialogTitle
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setPositiveButton

class SavePasswordHelpDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = createDialog(requireContext(), container)
        .setDialogTitle(R.string.login_save_password_help_title)
        .setDialogMessage(R.string.login_save_password_help_message)
        .setPositiveButton(R.string.login_save_password_help_button) {
            dismiss()
        }
}