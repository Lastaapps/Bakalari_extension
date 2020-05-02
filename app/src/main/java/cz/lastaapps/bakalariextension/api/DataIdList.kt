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

package cz.lastaapps.bakalariextension.api

/**Adds method to access data lists via their IDs*/
class DataIdList<T: DataID<*>>: ArrayList<T> {

    constructor(): super()

    constructor(list: List<T>): super(list)

    fun getById(id: Any?): T? {
        for (it in this) {
            if (it.id == id)
                return it
        }
        return null
    }

    fun getAllById(id: Any?): ArrayList<T> {
        val array = ArrayList<T>()
        for (it in this) {
            if (it.id == id)
                array.add(it)
        }
        return array
    }

    fun getByIds(list: List<Any>): T? {
        for (item in list) {
            return getById(item) ?: continue
        }
        return null
    }

    fun getAllByIds(list: List<Any>): ArrayList<T> {
        val array = ArrayList<T>()
        for (item in list) {
            array.addAll(getAllById(item))
        }
        return array
    }
}