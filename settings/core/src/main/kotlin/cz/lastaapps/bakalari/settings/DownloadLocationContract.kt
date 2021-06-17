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

package cz.lastaapps.bakalari.settings

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

class DownloadLocationContract() : ActivityResultContract<String, Uri?>() {

    companion object {
        private val TAG get() = DownloadLocationContract::class.simpleName
    }

    override fun createIntent(context: Context, input: String?): Intent {

        Toast.makeText(context, R.string.select_download_location, Toast.LENGTH_LONG).show()

        //    For the Samsung My files
        //    val intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
        //    intent.putExtra("CONTENT_TYPE", DocumentsContract.Document.MIME_TYPE_DIR)
        //    intent.addCategory(Intent.CATEGORY_DEFAULT)
        //    startActivityForResult(intent, REQUEST_CODE)

        val intent = when (Build.VERSION.SDK_INT) {

            in 21..28 -> Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

            else -> Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
        }
        //intent.addCategory(Intent.CATEGORY_OPENABLE)

        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return when (resultCode) {
            // Process the data received from second activity.
            RESULT_OK -> {
                val uri = intent!!.data!!

                Log.i(TAG, "Folder selected, uri: $uri")

                uri
            }
            else -> null
        }
    }
}