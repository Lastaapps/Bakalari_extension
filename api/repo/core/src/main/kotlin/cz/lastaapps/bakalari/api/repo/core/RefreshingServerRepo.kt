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
import kotlinx.coroutines.delay
import org.json.JSONObject


/**
 * Loads data from inputted api path
 * @param path in api/3/ to load data from*/
abstract class RefreshingServerRepo<E>(TAG: String, database: APIBase, private val path: String) :
    RefreshingRepoJSON<E>(TAG, database) {
    override suspend fun loadFromServer(): JSONObject? = loadFromServer(path)
}

/** Loads data from assets instead of loading them from server
 * @param path to asset file
 * @param delay before data return, simulates connection delay*/
abstract class RefreshingAssetRepo<E>(
    TAG: String,
    database: APIBase,
    private val path: String,
    private val delay: Long = 0
) : RefreshingRepoJSON<E>(TAG, database) {
    override suspend fun loadFromServer(): JSONObject? {
        delay(delay)
        return loadFromAssets(path)
    }
}