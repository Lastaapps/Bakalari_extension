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

package cz.lastaapps.bakalari.tools

import androidx.tracing.Trace


/**Class Trace.beginSection($tag $name), the code block and Trace.endSection()
 * @return data returned by the block*/
inline fun <E> runTrace(tag: String, name: String, block: () -> E): E =
    runTrace("$tag $name", block)

/**Class Trace.beginSection(name), the code block and Trace.endSection()
 * @return data returned by the block*/
inline fun <E> runTrace(name: String, block: () -> E): E {
    Trace.beginSection(name)
    val data = block()
    Trace.endSection()
    return data
}