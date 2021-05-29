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

package cz.lastaapps.bakalari.app.ui.user.homework

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cz.lastaapps.bakalari.api.repo.homework.HomeworkRepository
import cz.lastaapps.bakalari.api.repo.homework.homeworkRepository
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.tools.ui.RefreshableViewModel

class HmwViewModel : RefreshableViewModel<HomeworkRepository>(
    TAG,
    CurrentUser.requireDatabase().homeworkRepository
) {

    companion object {
        private val TAG = HmwViewModel::class.java.simpleName
    }

    /**Hold loaded homework*/
    val homework by lazy { repo.getAllHomeworkList().asLiveData() }

    val current by lazy { repo.getCurrentHomeworkList().asLiveData() }

    val old by lazy { repo.getOldHomeworkList().asLiveData() }

    suspend fun getHomeworkListForSubject(id: String) = repo.getHomeworkListForSubject(id)

    suspend fun getHomework(id: String) = repo.getHomework(id)

    suspend fun isCurrent(id: String) = repo.isCurrent(id)


    init {
        addEmptyObserver(homework.map { it })
    }


    /**The id of the homework to scroll to*/
    val selectedHomeworkId = MutableLiveData<String>()

    // Search fragment
    /**Text of the search field*/
    val searchText = MutableLiveData("")

    /**Index of selected subject*/
    val subjectIndex = MutableLiveData(0)

    /**If subjects spinner is currently used to filter data*/
    val searchingUsingSpinner = MutableLiveData(true)

    override fun emptyText(context: Context): String {
        return context.getString(R.string.homework_no_homework)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.homework_failed_to_load)
    }
}