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

package cz.lastaapps.bakalari.app.ui.user.subjects

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.subjects.SubjectRepository
import cz.lastaapps.bakalari.app.api.subjects.data.SubjectList
import cz.lastaapps.bakalari.app.api.subjects.data.TeacherList
import cz.lastaapps.bakalari.app.ui.uitools.RefreshableViewModel
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.app.ui.user.themes.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.set

class SubjectViewModel : RefreshableViewModel<SubjectRepository>(
    TAG,
    CurrentUser.requireDatabase().subjectTeacherRepository
) {

    companion object {
        private val TAG = SubjectViewModel::class.java.simpleName
    }

    val subjects by lazy { repo.getSubjects().asLiveData() }

    val teachers by lazy { repo.getTeachers().asLiveData() }

    suspend fun getSubject(id: String) = repo.getSubject(id)

    suspend fun getTeacher(id: String) = repo.getTeacher(id)

    suspend fun getSimpleTeacher(id: String) = repo.getSimpleTeacher(id)

    suspend fun getTeachersSubjects(id: String) = repo.getTeachersSubjects(id)

    init {
        viewModelScope.launch(Dispatchers.Main) {
            subjects.observeForever {
                it?.let {
                    isEmpty.value = it.isEmpty()
                }
            }
        }
    }

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

    fun requireSubjects(): SubjectList {
        return subjects.value!!
    }

    fun requireTeachers(): TeacherList {
        return teachers.value!!
    }

    fun executeTeachersOrRefresh(lifecycle: Lifecycle, todo: ((TeacherList) -> Unit)) {
        teachers.observe({ lifecycle }) { todo(it) }
        if (hasData.value != true || repo.shouldReload()/*runs auto update once only*/)
            loadData()
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.subject_teacher_no_data)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.subject_teacher_failed_to_load)
    }
}