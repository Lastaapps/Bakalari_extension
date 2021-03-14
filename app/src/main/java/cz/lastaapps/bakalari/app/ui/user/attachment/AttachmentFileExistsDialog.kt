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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.attachment.data.Attachment
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.createDialog
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setDialogItems
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setDialogTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**Show oder options if file cannot be downloaded directly*/
class AttachmentFileExistsDialog : BottomSheetDialogFragment() {

    companion object {
        private val TAG = AttachmentFileExistsDialog::class.java.simpleName
    }

    private val args: AttachmentFileExistsDialogArgs by navArgs()
    private lateinit var attachment: Attachment
    private lateinit var fileName: String

    private lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "Creating view")

        //loads data
        attachment = args.attachment
        fileName = args.fileName

        val newFileName = getFileNameWithNumber(fileName)

        //the list of options
        val selection = arrayListOf(
            getString(R.string.attachment_file_exists_open),
            String.format(getString(R.string.attachment_file_exists_rename), newFileName),
            getString(R.string.attachment_file_exists_cancel)
        )

        //replace available only it the app has permission to write to the file
        if (AttachmentDownload.accessible(requireContext(), fileName)) {
            selection.add(getString(R.string.attachment_file_exists_replace))
        }

        root = createDialog(requireContext(), container)
            .setDialogTitle(R.string.attachment_file_exists_label)
            .setDialogItems(selection) { position ->
                lifecycleScope.launch(Dispatchers.Main) {
                    when (position) {
                        //Open
                        0 -> {
                            val uri =
                                AttachmentDownload.getTargetUri(requireContext(), fileName, "")!!
                            val intent = AttachmentDownload.getIntent(
                                uri,
                                requireContext().contentResolver.getType(uri)!!
                            )
                            requireContext().startActivity(intent)
                        }
                        //Rename with number (1)
                        1 -> {
                            onDownloadClick(requireActivity(), attachment, newFileName, false)
                        }
                        //Close
                        2 -> {
                            dismiss()
                        }
                        //replace
                        3 -> {
                            onDownloadClick(requireActivity(), attachment, fileName, true)
                        }
                    }
                    dismiss()
                }
            }

        return root
    }

    /**creates filename with added number (in rename mode) name -> name (1)*/
    private fun getFileNameWithNumber(fileName: String): String {
        var index = 0
        while (true) {
            index++

            val name: String
            val extension: String
            //for files without extension
            if (fileName.contains(".")) {
                name = fileName.substring(0, fileName.lastIndexOf("."))
                extension = "." + fileName.substring(fileName.lastIndexOf(".") + 1)
            } else {
                name = fileName
                extension = ""
            }

            //creates file name
            val tempFileName = "$name ($index)$extension"

            //if file exists, continues in increasing index
            if (!AttachmentDownload.exists(requireContext(), tempFileName))
                return tempFileName
        }
    }
}