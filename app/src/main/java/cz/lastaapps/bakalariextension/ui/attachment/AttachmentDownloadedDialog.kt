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

package cz.lastaapps.bakalariextension.ui.attachment

import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.views.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


/**shows a popup to open downloaded file*/
class AttachmentDownloadedDialog : BottomSheetDialogFragment() {

    companion object {
        private val TAG = AttachmentDownloadedDialog::class.java.simpleName
    }

    private val args: AttachmentDownloadedDialogArgs by navArgs()

    private lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "Creating view")

        root = createDialog(requireContext(), container)
            .setDialogTitle(String.format(getString(R.string.attachment_done_title), args.fileName))
            .setDialogButton(BUTTON_NEGATIVE, R.string.attachment_done_dismiss) { dismiss() }
            .setDialogButton(BUTTON_POSITIVE, R.string.attachment_done_open) {
                startActivity(args.intent)
                dismiss()
            }

        lifecycleScope.launch(Dispatchers.IO) {

            if (Build.VERSION.SDK_INT >= 29) {

                val displayMetrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels
                val width = displayMetrics.widthPixels
                val size = min(width, height)

                val thumbnail =
                    requireContext().contentResolver.loadThumbnail(
                        args.uri,
                        Size(size, size),
                        CancellationSignal()
                    )

                Log.i(TAG, "Thumbnail loaded")

                withContext(Dispatchers.Main) {
                    root.setDialogImage { it.setImageBitmap(thumbnail) }
                }
            }
        }

        return root
    }
}
