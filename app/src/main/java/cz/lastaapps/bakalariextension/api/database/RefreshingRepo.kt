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

package cz.lastaapps.bakalariextension.api.database

import android.util.Log
import cz.lastaapps.bakalariextension.tools.runTrace
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/** Repository template common across all the API modules
 * @property D - data type returned by parser from JSON
 * @property J - data type returned by the #loadFromServer()
 *               and passed into #parseData() and #saveToJsonStorage()*/
abstract class RefreshingRepo<D, J>(private val TAG: String, database: APIBase) :
    APIRepo<D>(TAG, database) {

    /*
    *  l ^  d -> loading and showing data
    *  l ^ !d -> loading
    * !l ^  d -> showing data
    * !l ^ !d -> failed
    */

    /**state of #refreshData()*/
    private var refreshingChannel: ConflatedBroadcastChannel<Int>? = null

    /**synchronization in #refreshData()*/
    private val refreshMutex by lazy { Mutex() }

    /**Puts fresh data into flows to provide data to UI
     * before it is inserted and reconstructed from database*/
    private val newDataChannel = ConflatedBroadcastChannel<D>()

    /**Updates flow from database source and from newDataChannel.
     * Filled up with #distinctUntilChanged()*/
    protected fun Flow<D?>.onDataUpdated(): Flow<D?> = onDataUpdated() { it }
    protected fun <T> Flow<T>.onDataUpdated(converter: (D) -> T): Flow<T> = channelFlow<T> {
        launch {
            newDataChannel.openSubscription().consumeAsFlow().distinctUntilChanged().collect {
                send(converter(it))
            }
        }

        this@onDataUpdated.distinctUntilChanged().collect {
            send(it)
        }
    }.distinctUntilChanged()

    /**Refreshed data from server
     * @return StateFlow with current state LOADING, FAILED, SUCCEED */
    final override suspend fun refreshData(): ConflatedBroadcastChannel<Int> {
        refreshMutex.withLock {

            if (refreshingChannel == null) {

                refreshingChannel = ConflatedBroadcastChannel(LOADING)
                isLoading.send(true)

                database.getScope().launch(Dispatchers.IO) {

                    Log.i(TAG, "Refreshing data")

                    val json = runTrace(TAG, "Loading from server") { loadFromServer() }
                    yield()

                    if (json != null) {

                        //parses json
                        Log.i(TAG, "Parsing")
                        val data = withContext(Dispatchers.Default) {
                            runTrace(TAG, "Parsing") { parseData(json) }
                        }
                        yield()

                        Log.i(TAG, "Inserting into database")
                        database.tablesUpdated(
                            runTrace(TAG, "Inserting to database") {
                                insertIntoDatabase(data)
                            }
                        )

                        //data posted to UI after database insertion -
                        //database requires mutable variant nad the insertion is pretty fast
                        newDataChannel.send(data)
                        updateHasData()
                        refreshingChannel!!.send(SUCCEEDED)
                        dataUpdated.send(true)

                        Log.i(TAG, "Data refreshed")

                        database.getScope().launch(Dispatchers.IO) {
                            //let other data changes happen
                            delay(500)

                            saveToJsonStorage(database.jsonStorageRepository, json)
                        }

                    } else {
                        refreshingChannel!!.send(FAILED)

                        Log.e(TAG, "Failed to refresh data")
                    }

                    refreshMutex.withLock {
                        refreshingChannel!!.cancel()
                        refreshingChannel = null
                        isLoading.send(false)
                    }
                }

            } // else refreshing is already running

            return refreshingChannel!!
        }
    }

    /** @return json loaded from server*/
    protected abstract suspend fun loadFromServer(): J?

    /** Saves JSON into json storage table using JSONStorageRepo*/
    protected abstract suspend fun saveToJsonStorage(repo: JSONStorageRepository, json: J)

    /** Parses data into format needed*/
    protected abstract suspend fun parseData(json: J): D

    /** Inserts data to database
     * @return list of updated tables*/
    protected abstract suspend fun insertIntoDatabase(data: D): List<String>

}