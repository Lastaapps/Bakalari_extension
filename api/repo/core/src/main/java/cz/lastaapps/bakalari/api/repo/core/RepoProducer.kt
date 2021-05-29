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

package cz.lastaapps.bakalari.api.repo.core

import cz.lastaapps.bakalari.api.database.APIBase

abstract class RepoProducer<T : Deletable> {

    companion object {
        private val storage = mutableMapOf<APIBase, MutableList<Deletable>>()
    }

    fun getInstance(database: APIBase): T = synchronized(this) {

        val repos = storage.getOrPut(database) {
            mutableListOf<Deletable>().also {
                database.addOnDeleteAction { it.forEach { it.deleteAll() } }
                database.addOnCloseAction { storage.remove(database) }
            }
        }

        repos.forEach {
            if (isThisRepo(it)) {
                @Suppress("UNCHECKED_CAST")
                return@synchronized it as T
            }
        }

        createRepo(database).also {
            repos += it
        }
    }

    protected abstract fun createRepo(database: APIBase): T

    protected abstract fun isThisRepo(repo: Deletable): Boolean
}