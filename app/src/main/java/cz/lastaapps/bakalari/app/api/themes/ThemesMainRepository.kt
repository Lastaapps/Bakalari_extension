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

package cz.lastaapps.bakalari.app.api.themes

import cz.lastaapps.bakalari.app.api.database.APIBase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ThemesMainRepository(private val database: APIBase) {

    /**holds ThemeRepositories instances*/
    private val repoMap = HashMap<String, ThemesRepository>()

    /**@return ThemeRepository for subject given*/
    fun getRepository(subjectId: String): ThemesRepository {
        return if (repoMap.containsKey(subjectId)) {
            repoMap[subjectId]!!
        } else {
            ThemesRepository(database, subjectId).also {
                repoMap[subjectId] = it
            }
        }
    }

    /**Loads subjects ids from the SubjectTeacherRepository and the tries to refresh them all*/
    suspend fun refreshAll() {
        val subRepo = database.subjectTeacherRepository
        val subjects = subRepo.getSubjects().map { list -> list.map { it.id } }.first()

        for (id in subjects) {
            getRepository(id).refreshDataAndWait()
        }
    }

    private val dao = database.themeDao()

    fun getAllThemes() = dao.getAllThemes().map {
        cz.lastaapps.bakalari.app.api.themes.data.ThemeList(
            it
        )
    }.distinctUntilChanged()

    suspend fun deleteAll() = dao.deleteAll()

}