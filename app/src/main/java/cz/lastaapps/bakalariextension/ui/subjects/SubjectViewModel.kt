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

package cz.lastaapps.bakalariextension.ui.subjects

import android.content.Context
import androidx.lifecycle.MutableLiveData
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.subjects.SubjectList
import cz.lastaapps.bakalariextension.api.subjects.SubjectLoader
import cz.lastaapps.bakalariextension.api.subjects.TeacherList
import cz.lastaapps.bakalariextension.api.subjects.data.Teacher
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel

/**Holds data for teacher and subject modules and ViewModels for themes*/
class SubjectViewModel : RefreshableViewModel<SubjectList>(TAG) {

    companion object {
        private val TAG = SubjectViewModel::class.java.simpleName
    }

    init {
        data.observeForever {
            teachers.value = Teacher.subjectsToTeachers(it)
        }
    }

    /**List of subjects*/
    val subjects = data

    /**List of teachers*/
    val teachers = MutableLiveData<TeacherList>()

    /**holds ViewModels for themes*/
    private val themeMap = HashMap<String, ThemeViewModel>()

    /**@return ViewModel with themes for subject given*/
    fun getThemeViewModelForSubject(subjectId: String): ThemeViewModel {
        return if (themeMap.containsKey(subjectId)) {
            themeMap[subjectId]!!
        } else {
            ThemeViewModel(subjectId).also {
                themeMap[subjectId] = it
            }
        }
    }

    override suspend fun loadServer(): SubjectList? {
        return SubjectLoader.loadFromServer()
    }

    override suspend fun loadStorage(): SubjectList? {
        return SubjectLoader.loadFromStorage()
    }

    override fun shouldReload(): Boolean {
        return SubjectLoader.shouldReload()
    }

    override fun isEmpty(data: SubjectList): Boolean {
        return data.isEmpty()
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.teacher_no_teachers)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.teacher_failed_to_load)
    }
}