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

package cz.lastaapps.bakalariextension.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**Parent for ViewModels with loading and update of data from API*/
abstract class RefreshableViewModel<E>(val TAG: String) : ViewModel() {

    /**holds main data set*/
    val data = MutableLiveData<E>()

    /**if data loading failed*/
    val failed = MutableLiveData(false)

    /**if data are refreshing right now*/
    val isRefreshing = MutableLiveData(false)

    /**if there is no data*/
    val isEmpty = MutableLiveData(false)

    /**reloads data*/
    fun onRefresh(force: Boolean = false) {

        if (isRefreshing.value == true)
            return

        isRefreshing.value = true

        viewModelScope.launch(Dispatchers.Default) {

            Log.i(TAG, "Refreshing")

            var loaded: E?
            if (force) {
                loaded = loadServer().also {
                    if (it == null) Log.e(TAG, "Forced server loading failed!")
                }

            } else {

                loaded = loadStorage().also {
                    if (it == null) Log.i(TAG, "Storage loading failed!")
                }

                if (shouldReload() || loaded == null) {
                    loaded?.let {
                        withContext(Dispatchers.Main) {
                            updateData(it)
                        }
                    }

                    loaded = loadServer().also {
                        if (it == null) Log.i(TAG, "Server loading failed!")
                    }
                }
            }

            withContext(Dispatchers.Main) {

                updateData(loaded)

                //hides refreshing icon
                isRefreshing.value = false

                //shows Toast if there is no data to show on if user requested server update and it failed
                if (data.value == null || (force && loaded == null))
                    showToast()
            }
        }
    }

    /** updates views with data given*/
    private fun updateData(newData: E?) {

        Log.i(TAG, "Updating data to show in UI")

        failed.value = false

        //when download failed
        if (newData == null) {

            if (data.value == null) {
                failed.value = true
                isEmpty.value = false
            }
        } else {
            isEmpty.value = isEmpty(newData)

            //updates marks with new value
            data.value = newData
        }
    }

    /**@return data loaded from server*/
    protected abstract suspend fun loadServer(): E?

    /**@return data loaded from local storage*/
    protected abstract suspend fun loadStorage(): E?

    /**@return if data should be reloaded from server*/
    protected abstract fun shouldReload(): Boolean

    /**@return if data set is empty*/
    protected open fun isEmpty(data: E): Boolean = false

    /**Shows Toast with text from #failedText()*/
    protected open fun showToast() {
        val context = App.context
        Toast.makeText(context, failedText(context), Toast.LENGTH_LONG).show()
    }

    /**removes need to use !! in code all the time*/
    fun requireData(): E {
        return data.value!!
    }

    /**
     * Observes for data change and executes action on data update
     * if data != null executes right now
     * if data == null calls onRefresh(false)
     */
    fun executeOrRefresh(lifecycle: Lifecycle, todo: ((E) -> Unit)) {
        data.observe({ lifecycle }) { todo(it) }
        if (data.value == null)
            onRefresh()
    }

    /**@return text to be shown in UI when data set is empty*/
    open fun emptyText(context: Context): String = ""

    /**@return text to be shown in UI when no data can be obtained*/
    open fun failedText(context: Context): String = ""
}