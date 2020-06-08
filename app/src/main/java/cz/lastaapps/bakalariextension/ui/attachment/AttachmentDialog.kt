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
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.attachment.data.Attachment
import cz.lastaapps.bakalariextension.tools.MySettings

/**Shows the list on attachments and makes them available to download*/
class AttachmentDialog : DialogFragment() {

    companion object {
        const val FRAGMENT_TAG = "cz.lastaapps.bakalariextension.ui.attachment.AttachmentDialog"
        val TAG = AttachmentDialog::class.java.simpleName

        /**creates an instance of dialog*/
        fun newInstance(attachments: DataIdList<Attachment>): AttachmentDialog {
            val frag = AttachmentDialog()
            val args = Bundle()
            args.putSerializable("attachments", attachments)
            frag.arguments = args
            return frag
        }

        /**on item selected*/
        fun onDownloadClick(
            activity: Activity,
            attachment: Attachment,
            fileName: String,
            ignoreExist: Boolean
        ) {
            Log.i(TAG, "Attachment selected $fileName")

            //checks if download is possible
            if ((AttachmentDownload.exists(activity, fileName) && !ignoreExist)
                || !AttachmentDownload.accessible(activity, fileName)
            ) {

                //shows dialog with alternatives
                val dialog =
                    AttachmentFileExistsDialog.newInstance(attachment.fileName) { activity, fileName, ignoreExist ->
                        onDownloadClick(activity, attachment, fileName, ignoreExist)
                    }
                dialog.show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    AttachmentFileExistsDialog.FRAGMENT_TAG
                )

                return
            }

            //starts file download
            AttachmentDownload.download(
                activity,
                fileName,
                attachment.type,
                attachment.id
            )
        }
    }

    private lateinit var attachments: DataIdList<Attachment>

    //alternative show method
    fun show(activity: AppCompatActivity, tag: String?) {
        val sett = MySettings.withAppContext()
        if (sett.getDownloadLocation() == "") {
            sett.chooseDownloadDirectory(activity) {}
        } else
            super.show(activity.supportFragmentManager, tag)
    }

    /**creates dialog*/
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        Log.i(TAG, "Creating dialog")

        //gets attachment
        attachments = requireArguments().getSerializable("attachments")!! as DataIdList<Attachment>

        //list of filenames
        val filenames = Array(attachments.size) { "" }
        attachments.forEachIndexed { i, it ->
            filenames[i] = it.fileName
        }

        return AlertDialog.Builder(requireContext()).apply {

            setCancelable(true)
            setItems(filenames) { _, position ->
                onDownloadClick(
                    requireActivity(),
                    attachments[position],
                    attachments[position].fileName,
                    false
                )
            }
            setTitle(R.string.attachment_label)

        }.create().apply {

            //dialog is not dismissed on button click
            setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.attachment_change_location))
            { _, _ ->
                MySettings(requireContext()).chooseDownloadDirectory(requireActivity()) {}
            }
        }
    }
}