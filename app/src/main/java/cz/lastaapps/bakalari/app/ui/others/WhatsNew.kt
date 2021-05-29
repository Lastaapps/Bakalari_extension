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

package cz.lastaapps.bakalari.app.ui.others

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.tools.getVersionCode
import cz.lastaapps.bakalari.tools.getVersionName

class WhatsNew(val context: Context) {

    companion object {
        private val TAG = WhatsNew::class.java.simpleName

        private const val SP_KEY = "WHATS_NEW"
        private const val LAST_VERSION_SHOWN = "LAST_VERSION_SHOWN"
    }

    /**@return If What's new message was shown*/
    fun shouldShow(): Boolean {
        return context.getVersionCode() > getSP().getLong(
            LAST_VERSION_SHOWN, 0
        )
    }

    /**Shows dialog with whats new text and saves, that this version's updates was seen*/
    fun showDialog() {
        Log.i(TAG, "Showing dialog")

        //shows dialog with whats new text
        AlertDialog.Builder(context).apply {

            setCancelable(true)
            setPositiveButton(R.string.close) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            setTitle(R.string.whats_new_title)
            //shows beta updates to beta users only
            if (!context.getVersionName().lowercase().contains("beta")) {
                setMessage(R.string.whats_new)
            } else {
                setMessage(R.string.whats_new_beta)
            }
            show()
        }

        //message was seen
        getSP().edit {
            putLong(LAST_VERSION_SHOWN, context.getVersionCode())
        }
    }

    private fun getSP(): SharedPreferences {
        return context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
    }
}