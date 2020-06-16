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
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.HomeworkLoader
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class HmwViewModel : RefreshableViewModel<HomeworkList>() {

    companion object {
        private val TAG = HmwViewModel::class.java.simpleName
    }

    /**Hold loaded homework*/
    val homework = data

    /**The id of the homework to scroll to*/
    val selectedHomeworkId = MutableLiveData<String>()

    // Search fragment
    /**Text of the search field*/
    val searchText = MutableLiveData("")

    /**Index of selected subject*/
    val subjectIndex = MutableLiveData(0)

    /**When user refreshes marks with swipe from the top of the screen*/
    override fun onRefresh(force: Boolean) {

        if (isRefreshing.value == true)
            return

        //set state refreshing
        isRefreshing.value = true

        viewModelScope.launch(Dispatchers.Default) {

            Log.i(TAG, "Loading homework list")

            var loaded: HomeworkList?
            if (force) {
                loaded = HomeworkLoader.loadFromServer()

            } else {

                loaded = HomeworkLoader.loadFromStorage()

                if (HomeworkLoader.shouldReload() || loaded == null) {
                    loaded?.let {
                        withContext(Dispatchers.Main) {
                            homework.value = it
                        }
                    }

                    //let oder work finish before running slow loading from server
                    for (i in 0 until 10) yield()

                    loaded = HomeworkLoader.loadFromServer()
                }
            }

            withContext(Dispatchers.Main) {

                failed.value = false
                isEmpty.value = false

                //when download failed
                if (loaded == null) {
                    Log.e(TAG, "Download failed")

                    Toast.makeText(
                        App.context, R.string.homework_failed_to_load, Toast.LENGTH_LONG
                    ).show()

                    failed.value = true
                } else {
                    isEmpty.value = loaded.isEmpty()

                    //updates marks with new value
                    homework.value = loaded
                }

                //hides refreshing icon
                isRefreshing.value = false
            }
        }
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.homework_no_homework)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.homework_failed_to_load)
    }
}