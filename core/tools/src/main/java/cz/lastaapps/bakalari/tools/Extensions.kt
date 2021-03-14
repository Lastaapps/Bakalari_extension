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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject
import java.io.*


fun Serializable.toBytes(): ByteArray {
    val bos = ByteArrayOutputStream()
    val out: ObjectOutputStream?
    try {
        out = ObjectOutputStream(bos)
        out.writeObject(this)
        out.flush()
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

fun Parcelable.marshall(): ByteArray {
    val parcel = Parcel.obtain()
    this.writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.recycle() // not sure if needed or a good idea
    return bytes
}

fun <T : Parcelable> ByteArray.unmarshall(creator: Parcelable.Creator<T>): T {
    val parcel = unmarshallToParcel(this)
    return creator.createFromParcel(parcel)
}

fun unmarshallToParcel(bytes: ByteArray): Parcel {
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0) // this is extremely important!
    return parcel
}

fun <T : Comparable<T>, E : ArrayList<T>> E.sortList(): E {
    this.sort()
    return this
}

inline fun <reified T> JSONObject.getOrNull(key: String): T? =
    if (!isNull(key)) get(key) as T else null

fun JSONObject.getStringOrEmpty(key: String): String = getOrNull(key) ?: ""

fun Context.startForegroundServiceCompat(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        startForegroundService(intent)
    else
        startService(intent)
}

/**startActivityForResult() and others require 16-bit value - so we will use it then*/
fun Int.normalizeID(): Int = this and 0x0000FFFF

