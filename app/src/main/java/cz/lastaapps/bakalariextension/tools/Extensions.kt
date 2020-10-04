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

package cz.lastaapps.bakalariextension.tools

import org.json.JSONObject
import java.io.*

fun Serializable.toBytes(): ByteArray {
    val bos = ByteArrayOutputStream()
    var out: ObjectOutputStream? = null
    try {
        out = ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();
        return bos.toByteArray()

    } finally {
        try {
            bos.close();
        } catch (e: IOException) {
            // ignore close exception
        }
    }
}

fun <T> ByteArray.toSerializable(): T {
    val bis = ByteArrayInputStream(this)
    var input: ObjectInput? = null
    try {
        input = ObjectInputStream(bis)
        return input.readObject() as T
    } finally {
        try {
            input?.close()
        } catch (ex: IOException) {
            // ignore close exception
        }
    }
}

fun <T : Comparable<T>, E : ArrayList<T>> E.sortList(): E {
    this.sort()
    return this
}

inline fun <reified T> JSONObject.getOrNull(key: String): T? =
    if (!isNull(key)) get(key) as T else null

fun JSONObject.getStringOrEmpty(key: String): String = getOrNull(key) ?: ""

