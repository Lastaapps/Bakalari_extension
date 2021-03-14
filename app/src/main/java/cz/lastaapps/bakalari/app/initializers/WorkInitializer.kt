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

package cz.lastaapps.bakalari.app.initializers

import android.content.Context
import androidx.annotation.Keep
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import cz.lastaapps.bakalari.platform.InitializerTemplate

@Keep
class WorkInitializer : InitializerTemplate<WorkManager> {

    override fun create(context: Context): WorkManager {
        logCreate()

        WorkManager.initialize(context, Configuration.Builder().build())

        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        logDependencies()

        return emptyList()
    }
}