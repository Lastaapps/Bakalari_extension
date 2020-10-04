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
import androidx.annotation.CallSuper
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.api.ConnMgr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.ZonedDateTime


/** Repository template common across all the API modules
 * @property E - data type returned by parser from JSON*/
abstract class APIRepo<E>(private val TAG: String, protected val database: APIBase) {

    companion object {
        //results for #refreshData()
        const val LOADING = 0
        const val FAILED = -1
        const val SUCCEEDED = 1
    }

    /*
    *  l ^  d -> loading and showing data
    *  l ^ !d -> loading
    * !l ^  d -> showing data
    * !l ^ !d -> failed
    */
    /**if data is being loaded right now*/
    val isLoading by lazy { ConflatedBroadcastChannel(false) }

    /**if there is data cached from before*/
    val hasData by lazy { ConflatedBroadcastChannel(getHasData()) }

    /**updates when data was changed to observe for the update without need of the database data*/
    val dataUpdated by lazy { ConflatedBroadcastChannel(false) }

    /**determinate if there is data stored*/
    open fun getHasData(): Boolean = lastUpdated() != null

    protected suspend fun updateHasData() = hasData.send(getHasData())


    /**Refreshed data from server
     * @return StateFlow with current state LOADING, FAILED, SUCCEED */
    abstract suspend fun refreshData(): ConflatedBroadcastChannel<Int>

    /**@return when was data last updated*/
    fun lastUpdated(): ZonedDateTime? {
        var lowest: ZonedDateTime? = null

        val dates = database.tablesLastUpdated(lastUpdatedTables())
        for (entry in dates) {
            val date = entry.value ?: return null

            if (lowest == null || lowest > date) lowest = date
        }

        return lowest
    }

    /**If data should be reloaded from server*/
    fun shouldReload(): Boolean {
        val lastUpdated = lastUpdated() ?: return true
        return ZonedDateTime.now() > shouldReloadDelay(lastUpdated)
    }

    protected suspend fun loadFromServer(
        path: String,
        dataMap: Map<String, String> = HashMap()
    ): JSONObject? {
        return try {
            Log.i(TAG, "Loading from server")

            //downloads marks
            withContext(Dispatchers.IO) { ConnMgr.serverGet(path, dataMap) }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    protected suspend fun loadFromAssets(path: String): JSONObject? {
        return try {
            Log.i(TAG, "Loading from assets")

            //downloads marks
            withContext(Dispatchers.IO) {
                JSONObject(BufferedReader(InputStreamReader(App.context.assets.open(path))).readText())
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** update times of these tables are considered in #lastUpdated() method*/
    protected abstract fun lastUpdatedTables(): List<String>

    /**@return date and time increased with delay after which #schouldReload() returns true*/
    protected abstract fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime

    @CallSuper
    open suspend fun deleteAll() {
        hasData.send(false)
    }
}

