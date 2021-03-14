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

package cz.lastaapps.bakalari.app.ui.uitools

import android.content.Context
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalari.app.api.database.APIRepo
import cz.lastaapps.bakalari.app.api.subjects.SubjectRepository
import cz.lastaapps.bakalari.platform.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class RefreshableViewModel<R : APIRepo<*>>(val TAG: String, protected val repo: R) :
    EquippedViewModel() {

    /**if data loading failed*/
    val hasData by lazy { repo.hasData.asLiveData() }

    /**if data are refreshing right now*/
    val isLoading by lazy { repo.isLoading.asLiveData() }

    /**if loading failed or if there is no data and no attempt to get them is in process*/
    val isFailed by lazy { repo.isFailed.asLiveData() }

    /**Updates when data updated - meant for components not requiring data change,
     * update is enough, like lastUpdatedText texts*/
    val dataUpdated by lazy { repo.dataUpdated.asLiveData() }

    /**if there is no data*/
    val isEmpty = MutableLiveData(false)

    fun loadData() {
        viewModelScope.launch(Dispatchers.Default) {

            val refreshFlow = repo.refreshData()

            refreshFlow.collect {
                withContext(Dispatchers.Main) {

                    when (it) {
                        SubjectRepository.LOADING -> {
                        }
                        SubjectRepository.FAILED -> {
                            showToast()
                        }
                        SubjectRepository.SUCCEEDED -> {
                        }
                    }
                }
            }
        }
    }

    fun lastUpdated() = repo.lastUpdated()

    /**Observes the list in the LiveData given and updates #isEmpty() field based on List.isEmpty()*/
    protected fun addEmptyObserver(data: LiveData<List<*>>) {
        data.observeForever {
            it?.let {
                isEmpty.value = it.isEmpty()
            }
        }
    }

    /**Shows Toast with text from #failedText()*/
    @UiThread
    protected open fun showToast() {
        val context = App.context
        Toast.makeText(context, failedText(context), Toast.LENGTH_SHORT).show()
    }


    /**
     * Observes for data change and executes action on data update
     * if data != null executes right now
     * if data == null calls onRefresh(false)
     * then filters null values
     */
    fun <T> runOrRefresh(liveData: LiveData<T>, lifecycle: Lifecycle, todo: ((T) -> Unit)) {
        liveData.observe({ lifecycle }) { todo(it) }
        if (hasData.value != true || repo.shouldReload()/*runs auto update once only*/)
            loadData()
    }

    /**@see #runOnRefresh(liveData, lifecycle, (T) -> Unit
     * runs the code block using coroutines on the main thread*/
    fun <T> runOrRefresh(
        liveData: LiveData<T>,
        lifecycle: Lifecycle,
        scope: CoroutineScope,
        todo: suspend ((T) -> Unit)
    ) = runOrRefresh(liveData, lifecycle) { scope.launch(Dispatchers.Main) { todo(it) } }

    /**Called when new data was inserted into database,
     * they don't have to be updated yet in viewmodel's LiveData
     * @see #runOnRefresh(liveData, lifecycle, (T) -> Unit*/
    fun onDataUpdate(lifecycle: Lifecycle, todo: ((Boolean) -> Unit)) =
        runOrRefresh(dataUpdated, lifecycle, todo)

    /**Called when new data was inserted into database,
     * they don't have to be updated yet in viewmodel's LiveData
     * @see #runOnRefresh(liveData, lifecycle, (T) -> Unit
     * runs the code block using coroutines on the main thread*/
    fun onDataUpdate(
        lifecycle: Lifecycle,
        scope: CoroutineScope,
        todo: suspend ((Boolean) -> Unit)
    ) = runOrRefresh(dataUpdated, lifecycle, scope, todo)

    open fun emptyText(context: Context) = ""

    open fun failedText(context: Context) = ""
}

abstract class RefreshableDataViewModel<E, R : APIRepo<*>>(TAG: String, repo: R) :
    RefreshableViewModel<R>(TAG, repo) {

    abstract val data: LiveData<E>

    /**returns data without the need of !!*/
    @Deprecated("Unsafe", ReplaceWith("runOrRefresh(data, ...)"))
    fun requireData(): E = data.value!!

    /**waits until the data are available*/
    suspend fun waitForData(): E = waitFor(data)

    /**@see #runOnRefresh(LiveData, Lifecycle, (T) -> Unit
     * uses data as LiveData argument*/
    fun runOrRefresh(lifecycle: Lifecycle, todo: ((E) -> Unit)) =
        runOrRefresh(data, lifecycle, todo)

    /**@see #runOnRefresh(LiveData, Lifecycle, (T) -> Unit
     * uses data as LiveData argument
     * runs the code block using coroutines on the main thread*/
    fun runOrRefresh(
        lifecycle: Lifecycle,
        scope: CoroutineScope,
        todo: suspend ((E) -> Unit)
    ) = runOrRefresh(data, lifecycle, scope, todo)
}

abstract class RefreshableListViewModel<E : List<*>, R : APIRepo<*>>(TAG: String, repo: R) :
    RefreshableDataViewModel<E, R>(TAG, repo) {

    protected fun addEmptyObserver() {
        viewModelScope.launch(Dispatchers.Main) {
            data.observeForever {
                it?.let {
                    isEmpty.value = it.isEmpty()
                }
            }
        }
    }
}