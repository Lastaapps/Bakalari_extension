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

package cz.lastaapps.bakalari.app.ui.user

import android.content.Context
import android.util.Log
import androidx.work.*
import cz.lastaapps.bakalari.app.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalari.app.widgets.WidgetUpdater
import cz.lastaapps.bakalari.app.workers.WifiChargerWorker
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**Called when Main activity is loaded, inits services, widgets and alarms*/
class AppStartInit(val context: Context) {

    companion object {
        private val TAG = AppStartInit::class.java.simpleName
    }

    /**Inits services, widgets and alarms*/
    suspend fun appStartInit() {

        delay(3000)

        Log.i(TAG, "Running init of app's background")

        //runs all services needed
        launchServices(context)

        //updates widget
        WidgetUpdater.updateAndSetup(context)

        //deletes old timetables
        val database = CurrentUser.requireDatabase()
        for (day in database.timetableRepository.getWeeks().first()) {
            if (day < TimeTools.monday.toCzechDate()
                    .minusDays(2 * 7) && day != TimeTools.PERMANENT
            ) {
                database.timetableRepository.getRepositoryForDate(day).deleteAll()
            }
        }

        //sets up background workers
        setupWorkers(context)

        Log.i(TAG, "Init finished")
    }

    /**Launches all needed services*/
    private fun launchServices(context: Context) {
        TTNotifyService.startService(context)
    }

    /**updates background workers*/
    private fun setupWorkers(context: Context) {

        val manager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .build()

        // ...then create a OneTimeWorkRequest that uses those constraints
        val wifiChargerWork = PeriodicWorkRequestBuilder<WifiChargerWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
            .build()

        manager.enqueue(wifiChargerWork)
    }
}