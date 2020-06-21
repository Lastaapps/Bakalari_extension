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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**Parent for ViewModels with loading and update of data from API*/
abstract class RefreshableViewModel<E> : ViewModel() {

    /**holds main data set*/
    val data = MutableLiveData<E>()

    /**if data loading failed*/
    val failed = MutableLiveData(false)

    /**if data are refreshing right now*/
    val isRefreshing = MutableLiveData(false)

    /**if there is no data*/
    val isEmpty = MutableLiveData(false)

    /**reloads data*/
    abstract fun onRefresh(force: Boolean = false)

    /**removes need to use !! in code all the time*/
    open fun requireData(): E {
        return data.value!!
    }

    open fun emptyText(context: Context): String = ""
    open fun failedText(context: Context): String = ""
}