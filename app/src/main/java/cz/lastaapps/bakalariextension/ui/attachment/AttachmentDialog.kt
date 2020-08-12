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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.attachment.data.Attachment
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.ui.views.createDialog
import cz.lastaapps.bakalariextension.ui.views.setDialogButton
import cz.lastaapps.bakalariextension.ui.views.setDialogList
import cz.lastaapps.bakalariextension.ui.views.setDialogTitle

/**Shows the list on attachments and makes them available to download*/
class AttachmentDialog : BottomSheetDialogFragment() {

    companion object {
        val TAG = AttachmentDialog::class.java.simpleName
    }

    private val args: AttachmentDialogArgs by navArgs()

    private lateinit var attachments: DataIdList<Attachment>
    private lateinit var root: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sett = MySettings(requireContext())
        if (sett.getDownloadLocation() == "") {
            sett.chooseDownloadDirectory(requireActivity())
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        //gets attachment
        attachments = DataIdList(args.attachments)

        root = createDialog(requireContext(), container)
            .setDialogTitle(R.string.attachment_label)
            .setDialogButton(R.id.positive, R.string.attachment_change_location) {
                MySettings(requireContext()).chooseDownloadDirectory(requireActivity())
                dismiss()
            }
            .setDialogList {
                it.adapter = AttachmentAdapter(attachments) {
                    onDownloadClick(requireActivity(), it, it.fileName, false)
                }
            }

        return root
    }

}

/**on item selected*/
fun onDownloadClick(
    activity: Activity,
    attachment: Attachment,
    fileName: String,
    ignoreExist: Boolean
) {
    Log.i(AttachmentDialog.TAG, "Attachment selected $fileName")

    //checks if download is possible
    if ((AttachmentDownload.exists(activity, fileName) && !ignoreExist)
        || !AttachmentDownload.accessible(activity, fileName)
    ) {

        //shows dialog with alternatives
        val action = AttachmentDialogDirections.actionNavAttachmentToAttachmentFileExistsDialog(
            attachment,
            fileName
        )
        activity.findNavController(R.id.nav_host_fragment).navigate(action)

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