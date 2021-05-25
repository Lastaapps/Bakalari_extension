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
import androidx.startup.Initializer
import cz.lastaapps.bakalari.api.core.ModuleConfig
import cz.lastaapps.bakalari.platform.InitializerTemplate
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import kotlin.reflect.KClass

class APIInitializer : InitializerTemplate<Any> {

    @Suppress("PrivatePropertyName")
    private val TAG = this::class.simpleName

    override fun create(context: Context): Any {
        logCreate()

        ModuleConfig.initialize { MySettings.withAppContext().getNewMarkDuration() }

        return Any()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        logDependencies()

        //all the app's initializers
        return listOf<KClass<out Initializer<*>>>().map { it.java }
    }
}