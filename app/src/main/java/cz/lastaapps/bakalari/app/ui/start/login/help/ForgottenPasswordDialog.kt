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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.tools.ui.customdialog.*

class ForgottenPasswordDialog : BottomSheetDialogFragment() {

    val args: ForgottenPasswordDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = createDialog(requireContext(), container)

        root.setDialogTitle(R.string.login_forgotten_password_title)
        root.setDialogMessage(R.string.login_forgotten_password_message)
        root.setDialogButton(BUTTON_POSITIVE, R.string.login_forgotten_password_positive) {
            val url = args.url + "/next/serpwd.aspx"
            val uri = Uri.parse(url)
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, uri))
            dismiss()
        }
        root.setDialogButton(BUTTON_NEGATIVE, R.string.login_forgotten_password_negative) {

            dismiss()
        }

        return root
    }
}