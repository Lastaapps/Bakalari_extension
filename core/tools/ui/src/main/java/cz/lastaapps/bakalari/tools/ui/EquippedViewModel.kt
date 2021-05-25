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

package cz.lastaapps.bakalari.tools.ui

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow

/**Holds some useful methods specific to ViewModel*/
open class EquippedViewModel : ViewModel() {

    fun <E> waitAsync(
        scope: CoroutineScope,
        @WorkerThread getData: suspend () -> E,
        @MainThread todo: suspend (E) -> Unit,
    ) {
        scope.launch(Dispatchers.Default) {
            val data: E = getData()
            withContext(Dispatchers.Main) { todo(data) }
        }
    }

    /**waits until the data are available*/
    suspend fun <E> waitFor(liveData: LiveData<E>): E =
        suspendCancellableCoroutine<E> { continuation ->
            var observer: ((E) -> Unit) = {}

            observer = { it: E ->
                if (it != null) {
                    liveData.removeObserver(observer)
                    continuation.resumeWith(Result.success(it))
                }
            }
            liveData.observeForever(observer)

            continuation.invokeOnCancellation {
                liveData.removeObserver(observer)
            }
        }

    protected fun <E> Flow<E>.asLiveData(): LiveData<E> =
        this.asLiveData(viewModelScope.coroutineContext)

    protected fun <E> ConflatedBroadcastChannel<E>.asLiveData(): LiveData<E> {
        val liveData = MutableLiveData(value)

        viewModelScope.launch(Dispatchers.Default) {
            this@asLiveData.consumeEach {
                liveData.postValue(it)
            }
        }

        return liveData
    }

    @MainThread
    protected fun <E> Flow<E>.observe(action: (E) -> Unit) {
        asLiveData().observeForever(action)
    }
}