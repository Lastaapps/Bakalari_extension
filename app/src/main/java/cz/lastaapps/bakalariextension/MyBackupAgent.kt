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

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import cz.lastaapps.bakalariextension.login.LoginData

/**Backs up login (sort off), licence and preferences to Google drive*/
class MyBackupAgent : BackupAgent() {
    companion object {
        private val TAG = MyBackupAgent::class.java.simpleName
    }

    //backup app immediately
    //adb shell bmgr backupnow cz.lastaapps.bakalariextension

    override fun onBackup(
        oldState: ParcelFileDescriptor,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor
    ) {
        Log.i(TAG, "Creating backup")
    }

    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        Log.i(TAG, "Restoring backup")
    }

    override fun onRestoreFinished() {
        super.onRestoreFinished()

        Log.i(TAG, "Restore finished")

        App.tempContext = this

        //if refreshing the token failed
        LoginData.refreshToken = ""
        LoginData.accessToken = ""
        LoginData.tokenExpiration = 0

        App.tempContext = null
    }
}