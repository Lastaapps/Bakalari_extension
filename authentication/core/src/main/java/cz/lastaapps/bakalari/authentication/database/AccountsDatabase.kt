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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*

@Database(
    entities = [BakalariAccount::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(AccountsDatabase.Converter::class)
abstract class AccountsDatabase : RoomDatabase() {

    companion object {

        const val BAKALARI_ACCOUNT = "Bakalari_account"

        private val TAG = AccountsDatabase::class.simpleName
        private const val DATABASE_NAME = "ACCOUNTS"

        private var instance: AccountsDatabase? = null

        fun getDatabase(context: Context): AccountsDatabase = synchronized(this) {
            instance?.let { return@synchronized it }

            val scope = CoroutineScope(Dispatchers.Default)

            return@synchronized Room.databaseBuilder(
                context,
                AccountsDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(APIBaseCallback(scope))
                //TODO database migration
                .fallbackToDestructiveMigration()
                .build().also {
                    it.coroutineScope = scope
                    instance = it
                }
        }
    }

    private lateinit var coroutineScope: CoroutineScope

    protected abstract fun accountsDao(): AccountsDao

    val repository by lazy { AccountsRepository(this, accountsDao()) }


    private class APIBaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Creating accounts database")
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Opening accounts database")
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            Log.e(TAG, "Destructing accounts database")
        }
    }

    class Converter {

        @TypeConverter
        fun instantToLong(instant: Instant?) = instant?.epochSecond

        @TypeConverter
        fun longToInstant(long: Long?) = long?.let { Instant.ofEpochSecond(it) }

        @TypeConverter
        fun fromTimestampZoned(value: Long?): ZonedDateTime? =
            value?.let {
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(value), TimeTools.CET).toCommon()
            }

        @TypeConverter
        fun zonedToTimestamp(date: ZonedDateTime?): Long? = date?.toInstant()?.epochSecond

        @TypeConverter
        fun fromTimestampDate(value: Long?): LocalDate? =
            value?.let { LocalDate.ofEpochDay(value) }

        @TypeConverter
        fun dateToTimestamp(date: LocalDate?): Long? = date?.toEpochDay()

        @TypeConverter
        fun fromTimestampTime(value: Int?): LocalTime? =
            value?.let { LocalTime.ofSecondOfDay(value.toLong()) }

        @TypeConverter
        fun timeToTimestamp(date: LocalTime?): Int? = date?.toSecondOfDay()

        @TypeConverter
        fun uuidToString(uuid: UUID?): String? = uuid?.toString()

        @TypeConverter
        fun stringToUUID(string: String?): UUID? = string?.let { UUID.fromString(it) }

        @TypeConverter
        fun uriToString(uri: Uri?): String? = uri?.toString()

        @TypeConverter
        fun stringToUri(string: String?): Uri? = string?.let { Uri.parse(it) }
    }
}