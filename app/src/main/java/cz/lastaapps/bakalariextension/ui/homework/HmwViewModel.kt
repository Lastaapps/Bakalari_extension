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

package cz.lastaapps.bakalariextension.ui.homework

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.HomeworkLoader
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HmwViewModel : ViewModel() {

    companion object {
        private val TAG = HmwViewModel::class.java.simpleName
    }

    /**Hold loaded homework*/
    val homework = MutableLiveData<HomeworkList>()

    /**If SwipeRefreshLayouts are refreshing now*/
    val isRefreshing = MutableLiveData(false)

    /**Notifies root fragment, that no data will be shows*/
    val failObserve = MutableLiveData<Any>()

    /**The id of the homework to scroll to*/
    val selectedHomeworkId = MutableLiveData<String>()

    /**When user refreshes marks with swipe from the top of the screen*/
    fun onRefresh(context: Context, forceReload: Boolean = false) {

        if (isRefreshing.value == true)
            return

        //set state refreshing
        isRefreshing.value = true

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            Log.i(TAG, "Loading homework list")

            var loaded: HomeworkList?
            if (forceReload) {
                loaded = HomeworkLoader.loadFromServer()

            } else {

                loaded = HomeworkLoader.loadFromStorage()

                if (HomeworkLoader.shouldReload() || loaded == null) {
                    loaded?.let {
                        withContext(Dispatchers.Main) {
                            homework.value = it
                        }
                    }

                    loaded = HomeworkLoader.loadFromServer()
                }
            }

            withContext(Dispatchers.Main) {

                //when download failed
                if (loaded == null) {
                    Toast.makeText(
                        context, R.string.homework_failed_to_load, Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "Download failed")
                    failObserve.value = Any()
                } else {

                    //updates marks with new value
                    homework.value = loaded
                }

                //hides refreshing icon
                isRefreshing.value = false
            }
        }
    }

    // Search fragment
    /**Text of the search field*/
    val searchText = MutableLiveData("")

    /**Index of selected subject*/
    val subjectIndex = MutableLiveData(0)

}