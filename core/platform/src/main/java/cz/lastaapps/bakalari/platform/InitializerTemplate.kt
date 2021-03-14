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

package cz.lastaapps.bakalari.platform

import android.util.Log
import androidx.startup.Initializer

interface InitializerTemplate<T> : Initializer<T> {
    fun logCreate() = Log.i(this::class.simpleName, "Initializers -  Running the initializer")
    fun logDependencies() =
        Log.d(this::class.simpleName, "Initializers -  Getting the dependencies")
}

/* To copy when creating new a initializer

@Keep

 : InitializerTemplate<Any> {

    override fun create(c: Context): Any {
        logCreate()

        return Any()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        logDependencies()

        return emptyList()
    }
}
*/

