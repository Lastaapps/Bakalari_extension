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

package cz.lastaapps.bakalari.app.ui.user.attachment

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.tools.ui.customdialog.*
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

        val intent = args.intent

        root = createDialog(requireContext(), container)
            .setDialogTitle(String.format(getString(R.string.attachment_done_title), args.fileName))
            .setDialogButton(BUTTON_NEGATIVE, R.string.attachment_done_dismiss) { dismiss() }

        val openFile = { intent: Intent ->
            try {
                //dismisses the notification informing about the successful download
                NotificationManagerCompat.from(requireContext())
                    .cancel(AttachmentDownload.getNotificationId(args.fileName))

                startActivity(intent)
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    R.string.attachment_done_cannot_open,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        //gets number of activities able to open selected file
        //on Android 11 and higher call returns no activities, so both open options are shown later
        val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Int.MAX_VALUE
        else {
            val activities: List<ResolveInfo> =
                requireContext().packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            activities.size
        }

        when (size) {
            //file cannot be opened
            0 -> {
            }
            //file can be opened only by one app
            1 -> {
                root.setDialogButton(BUTTON_POSITIVE, R.string.attachment_done_open) {
                    openFile(intent)
                }
            }
            //more apps can open the file, showing chooser option
            else -> {
                root.setDialogButton(BUTTON_POSITIVE, R.string.attachment_done_open) {
                    openFile(intent)
                }.setDialogButton(BUTTON_NEUTRAL, R.string.attachment_done_open_with) {
                    val chooser = Intent.createChooser(intent, null)
                    openFile(chooser)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {

            if (Build.VERSION.SDK_INT >= 29) {

                try {
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create thumbnail: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        return root
    }
}
