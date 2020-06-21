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

package cz.lastaapps.bakalariextension.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.user.UserLoader
import cz.lastaapps.bakalariextension.tools.TimeTools

/**Runs some tasks on charger when not metered connection to internet is available*/
class WifiChargerWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        private val TAG = WifiChargerWorker::class.java.simpleName
    }

    override suspend fun doWork(): Result {

        Log.i(TAG, "Working...")

        //if all actions succeed
        var success = true

        UserLoader.loadFromServer()

        //updates current timetable
        if (TimetableLoader.loadFromServer(TimeTools.monday) == null)
            success = false

        //loads timetable for the next week
        if (TimetableLoader.loadFromServer(TimeTools.monday.plusDays(7)) == null)
            success = false

        return if (success)
            Result.success()
        else
            Result.retry()
    }
}