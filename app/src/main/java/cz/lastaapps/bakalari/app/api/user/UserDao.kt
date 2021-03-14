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

package cz.lastaapps.bakalari.app.api.user

import androidx.room.*
import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.app.api.database.ClassData
import cz.lastaapps.bakalari.app.api.user.data.ModuleFeature
import cz.lastaapps.bakalari.app.api.user.data.User
import cz.lastaapps.bakalari.app.api.user.data.UserHolder
import cz.lastaapps.bakalari.app.api.user.data.UserHolderWithLists
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDao {

    @Query("SELECT COUNT(uid) FROM ${APIBase.USER}")
    abstract fun getRowCount(): Int

    @Transaction
    @Query("SELECT * FROM ${APIBase.USER} LIMIT 1")
    abstract fun getUser(): Flow<UserHolderWithLists?>

    @Transaction
    open suspend fun replace(user: User) {
        deleteAll()
        insert(user)
    }

    @Transaction
    protected open suspend fun insert(user: User) {
        insertClass(ClassData(user.classInfo))
        insertUser(UserHolder.fromUser(user))
        insertModules(user.modulesFeatures)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertClass(classData: ClassData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUser(user: UserHolder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertModules(modules: List<ModuleFeature>)

    @Transaction
    open suspend fun deleteAll() {
        deleteUser()
        deleteModules()
    }

    @Query("DELETE FROM ${APIBase.USER}")
    abstract suspend fun deleteUser()

    @Query("DELETE FROM ${APIBase.USER_MODULE}")
    abstract suspend fun deleteModules()
}