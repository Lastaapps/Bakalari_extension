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

package cz.lastaapps.bakalari.authentication.database

import androidx.room.*
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
abstract class AccountsDao {

    companion object {
        private const val table = AccountsDatabase.BAKALARI_ACCOUNT
    }

    @Query("SELECT * FROM $table WHERE uuid=:uuid LIMIT 1")
    abstract suspend fun getByUUID(uuid: UUID): BakalariAccount?

    @Query("SELECT * FROM $table WHERE profileName=:displayName LIMIT 1")
    abstract suspend fun getByDisplayName(displayName: String): BakalariAccount?

    @Query("SELECT * FROM $table WHERE autoLaunch=1 LIMIT 1")
    abstract suspend fun getAutoStart(): BakalariAccount?

    @Query("SELECT * FROM $table WHERE autoLaunch=1")
    abstract suspend fun getAllAutoStart(): List<BakalariAccount>

    @Query("SELECT * FROM $table ORDER BY `order`")
    abstract suspend fun getAll(): List<BakalariAccount>

    @Query("SELECT * FROM $table ORDER BY `order`")
    abstract fun getAllObservable(): Flow<List<BakalariAccount>>

    @Query("SELECT COUNT(uuid) FROM $table")
    abstract suspend fun getAccountsNumber(): Int

    @Query("SELECT EXISTS(SELECT uuid FROM $table WHERE uuid=:uuid LIMIT 1)")
    abstract suspend fun exitsUUID(uuid: UUID): Boolean

    @Query("SELECT NOT EXISTS(SELECT profileName FROM $table WHERE LOWER(profileName)=LOWER(:profileName) LIMIT 1)") //NOT - makes no sense, but works
    abstract suspend fun existsProfileName(profileName: String): Boolean

    @Query("SELECT NOT EXISTS(SELECT userName, url FROM $table WHERE LOWER(userName)=LOWER(:userName) AND LOWER(url)=LOWER(:url) LIMIT 1)")  //NOT - makes no sense, but works
    abstract suspend fun existsAccount(userName: String, url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(accounts: List<BakalariAccount>)

    suspend inline fun insert(account: BakalariAccount) = insert(listOf(account))

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(accounts: List<BakalariAccount>)

    suspend inline fun update(account: BakalariAccount) = update(listOf(account))

    suspend inline fun delete(account: BakalariAccount) = delete(account.uuid)

    @Query("DELETE FROM $table WHERE uuid=:uuid")
    abstract suspend fun delete(uuid: UUID)

    @Query("UPDATE $table SET tokenExpiration=0")
    abstract suspend fun expireTokens()
}