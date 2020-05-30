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

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import cz.lastaapps.bakalariextension.R

/**Show oder options if file cannot be downloaded directly*/
class AttachmentFileExistsDialog : DialogFragment() {

    companion object {
        const val FRAGMENT_TAG =
            "cz.lastaapps.bakalariextension.ui.attachment.AttachmentFileExistsDialog"
        val TAG = AttachmentFileExistsDialog::class.java.simpleName

        /**creates new dialog instance*/
        fun newInstance(
            fileName: String,
            callback: ((activity: Activity, fileName: String, ignoreExist: Boolean) -> Unit)
        ): AttachmentFileExistsDialog {

            val frag = AttachmentFileExistsDialog()
            //passes data into dialog
            val args = Bundle()
            args.putString("filename", fileName)
            frag.arguments = args
            Companion.callback = callback

            return frag
        }

        private var callback: ((activity: Activity, fileName: String, ignoreExist: Boolean) -> Unit)? =
            null
    }

    private lateinit var fileName: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        //loads data
        fileName = requireArguments().getString("filename")!!
        val newFileName = getFileNameWithNumber(fileName)

        //if app can write file, i can replace it
        val selection = if (AttachmentDownload.accessible(requireContext(), fileName)) {
            arrayOf(
                getString(R.string.attachment_file_exists_open),
                String.format(getString(R.string.attachment_file_exists_rename), newFileName),
                getString(R.string.attachment_file_exists_cancel),
                getString(R.string.attachment_file_exists_replace)
            )
        } else {
            arrayOf(
                getString(R.string.attachment_file_exists_open),
                String.format(getString(R.string.attachment_file_exists_rename), newFileName),
                getString(R.string.attachment_file_exists_cancel)
            )
        }

        //sets up dialog
        return AlertDialog.Builder(requireContext()).apply {

            setCancelable(true)
            setTitle(R.string.attachment_file_exists_label)
            setItems(selection) { _, position ->
                when (position) {
                    0 -> {
                        callback?.let {
                            val uri = AttachmentDownload.getTargetUri(context, fileName, "")!!
                            val intent = AttachmentDownload.getIntent(
                                uri,
                                context.contentResolver.getType(uri)!!
                            )
                            context.startActivity(intent)
                        }
                    }
                    1 -> {
                        callback?.let {
                            it(requireActivity(), newFileName, false)
                        }
                    }
                    2 -> {
                        dismiss()
                    }
                    3 -> {
                        callback?.let {
                            it(requireActivity(), fileName, true)
                        }
                    }
                }
            }
            setTitle(R.string.attachment_label)

        }.create()
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