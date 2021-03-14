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

package cz.lastaapps.bakalari.app.api.database

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cz.lastaapps.bakalari.tools.runTrace
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
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
    private var refreshingChannel: MutableStateFlow<Int>? = null

    /**synchronization in #refreshData()*/
    private val refreshMutex by lazy { Mutex() }

    /**Puts fresh data into flows to provide data to UI
     * before it is inserted and reconstructed from database*/
    private val newDataChannel = MutableSharedFlow<D>(1, 0, BufferOverflow.DROP_OLDEST)

    /**Updates flow from database source and from newDataChannel.
     * Filled up with #distinctUntilChanged()*/
    protected fun Flow<D?>.onDataUpdated(): Flow<D?> = onDataUpdated { it }
    protected fun <T> Flow<T>.onDataUpdated(converter: (D) -> T): Flow<T> = channelFlow {
        launch {
            newDataChannel.collect { send(converter(it)) }
        }

        this@onDataUpdated.distinctUntilChanged().collect { send(it) }
    }.distinctUntilChanged()

    /**Refreshed data from server
     * @return StateFlow with current state LOADING, FAILED, SUCCEED */
    final override suspend fun refreshData(): StateFlow<Int> {
        refreshMutex.withLock {

            if (refreshingChannel == null) {

                refreshingChannel = MutableStateFlow(LOADING)
                isLoading.value = true

                database.getScope().launch(Dispatchers.IO) {

                    try {
                        refreshDataImpl()
                    } catch (e: Exception) {

                        refreshingChannel!!.value = FAILED

                        Log.e(TAG, "Failed to refresh data - internal error: ${e.message}")

                        FirebaseCrashlytics.getInstance().recordException(e)
                        e.printStackTrace()
                    }

                    refreshMutex.withLock {
                        refreshingChannel = null
                        isLoading.value = false
                    }
                }

            } // else refreshing is already running

            return refreshingChannel!!
        }
    }

    private suspend fun refreshDataImpl() {

        Log.i(TAG, "Refreshing data")

        val json = runTrace(TAG, "Loading from server") { loadFromServer() }
        yield()

        if (json != null) {

            //saves even invalid data - the invalid data can be then reported
            database.getScope().launch(Dispatchers.IO) {
                //let other data changes happen
                delay(2000)

                try {
                    saveToJsonStorage(database.jsonStorageRepository, json)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save the json: ${e.message}")
                    e.printStackTrace()
                }
            }

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
            newDataChannel.tryEmit(data)
            updateHasData()
            refreshingChannel!!.value = SUCCEEDED
            dataUpdated.value = true

            Log.i(TAG, "Data refreshed")

        } else {
            refreshingChannel!!.value = FAILED

            Log.e(TAG, "Failed to refresh data")
        }
    }

    /**Calls #refreshData() and then waits, until the stream closes*/
    suspend fun refreshDataAndWait(): Int {
        val stream = refreshData()
        var lastValue: Int
        while (stream.first()
                .also { lastValue = it } == LOADING
        ) println("Stack in while"); //TODO remove

        return lastValue
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