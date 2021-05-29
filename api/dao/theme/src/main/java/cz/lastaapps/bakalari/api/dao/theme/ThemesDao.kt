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

package cz.lastaapps.bakalari.api.dao.theme

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.themes.Theme
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ThemesDao {

    @Query("SELECT * FROM ${APIBaseKeys.THEMES}")
    abstract fun getAllThemes(): Flow<List<Theme>>

    @Query("SELECT * FROM ${APIBaseKeys.THEMES} WHERE subjectId=:subjectId")
    abstract fun getThemes(subjectId: String): Flow<List<Theme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertThemes(list: List<Theme>)

    @Query("DELETE FROM ${APIBaseKeys.THEMES}")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM ${APIBaseKeys.THEMES} WHERE subjectId=:subjectId")
    abstract suspend fun deleteSubject(subjectId: String)
}