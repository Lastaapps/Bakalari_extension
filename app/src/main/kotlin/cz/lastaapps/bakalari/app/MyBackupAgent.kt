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

package cz.lastaapps.bakalari.app

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import cz.lastaapps.bakalari.platform.App

/**Backs up login (sort off), license and preferences to Google drive*/
class MyBackupAgent : BackupAgent() {
    companion object {
        private val TAG get() = MyBackupAgent::class.java.simpleName
    }

    //backup app immediately
    //adb shell bmgr backupnow cz.lastaapps.bakalari.app

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

        //TODO backup

        App.tempContext = null
    }
}