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

package cz.lastaapps.bakalariextension

import android.app.backup.BackupManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**Things related to viewing and agreeing licence*/
class Licence {

    companion object {
        private val TAG = Licence::class.java.simpleName

        private const val SP_KEY = "LICENCE"
        private const val SP_AGREED = "1.0"
        private const val SP_TIME = "1.0_date"

        /**@return if user has agreed to licence*/
        fun check(): Boolean {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).getBoolean(SP_AGREED, false)
        }

        /**called when user has agreed to licence*/
        private fun agreed() {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putBoolean(SP_AGREED, true)
                putLong(SP_TIME, ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli())
                apply()
            }

            //notifies that backup should be made
            BackupManager.dataChanged(App.context.packageName)
        }

        /**Shows dialog, which asks user to agree the licence*/
        fun showDialog(context: Context, run: Runnable) {
            AlertDialog.Builder(context)
                .setCancelable(false)
                .setMessage(R.string.licence_agreement)
                .setPositiveButton(R.string.agree)
                    { _: DialogInterface, _: Int ->
                        agreed()
                        run.run()
                    }
                .setNegativeButton(R.string.view_licence) {
                        _: DialogInterface, _: Int ->
                    viewLicence(context)
                }
                .setTitle(R.string.licence)
                .create()
                .show()
        }

        /**Shows GNU licence to user*/
        fun viewLicence(context: Context) {
            val url = "https://github.com/Lastaapps/Bakalari_extension/blob/master/LICENCE.md"
            val uri = Uri.parse(url)
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

}
