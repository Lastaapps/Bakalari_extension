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

package cz.lastaapps.bakalari.app.api.user

import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.app.api.database.JSONStorageRepository
import cz.lastaapps.bakalari.app.api.database.RefreshingServerRepo
import cz.lastaapps.bakalari.app.api.user.data.Semester
import cz.lastaapps.bakalari.app.api.user.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.ZonedDateTime

class UserRepository(database: APIBase) : RefreshingServerRepo<User>(TAG, database, "user") {
    //: APIAssetRepo<User>(TAG, database, "user.json", 500) {

    companion object {
        private val TAG = UserRepository::class.java.simpleName
    }

    private val dao = database.userDao()

    init {
        var firstSeen = false
        var lastSeen: Semester? = null //for debugging
        database.getScope().launch(Dispatchers.Default) {
            getUser().filterNotNull().map { it.semester }.distinctUntilChanged().collect {

                firstSeen = if (firstSeen) {
                    UserChangeObserver.onNew()
                    false
                } else {
                    true
                }
                lastSeen = it
            }
        }
    }

    fun getUser() = dao.getUser().distinctUntilChanged().map { it?.toUser() }.onDataUpdated()

    override fun lastUpdatedTables(): List<String> = listOf(APIBase.USER, APIBase.USER_MODULE)

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime = date.plusDays(7)

    override suspend fun parseData(json: JSONObject): User = UserParser.parseJson(json)

    override suspend fun insertIntoDatabase(data: User): List<String> {
        dao.replace(data)
        return lastUpdatedTables()
    }

    override suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: JSONObject) =
        repo.saveUser(json)

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }

    override fun getHasData(): Boolean =
        runBlocking(Dispatchers.Default) { dao.getRowCount() != 0 }
}