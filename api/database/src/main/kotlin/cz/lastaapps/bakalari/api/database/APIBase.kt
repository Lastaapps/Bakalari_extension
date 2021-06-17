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

package cz.lastaapps.bakalari.api.database

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.lastaapps.bakalari.api.dao.absence.AbsenceDao
import cz.lastaapps.bakalari.api.dao.absence.ThresholdHolder
import cz.lastaapps.bakalari.api.dao.event.EventsDao
import cz.lastaapps.bakalari.api.dao.homework.HomeworkAttachmentRelation
import cz.lastaapps.bakalari.api.dao.homework.HomeworkDao
import cz.lastaapps.bakalari.api.dao.homework.HomeworkHolder
import cz.lastaapps.bakalari.api.dao.mark.MarksDao
import cz.lastaapps.bakalari.api.dao.subject.SubjectDao
import cz.lastaapps.bakalari.api.dao.theme.ThemesDao
import cz.lastaapps.bakalari.api.dao.timetable.*
import cz.lastaapps.bakalari.api.dao.user.UserDao
import cz.lastaapps.bakalari.api.dao.user.UserHolder
import cz.lastaapps.bakalari.api.entity.absence.AbsenceDay
import cz.lastaapps.bakalari.api.entity.absence.AbsenceSubject
import cz.lastaapps.bakalari.api.entity.attachment.Attachment
import cz.lastaapps.bakalari.api.entity.core.*
import cz.lastaapps.bakalari.api.entity.events.*
import cz.lastaapps.bakalari.api.entity.marks.Mark
import cz.lastaapps.bakalari.api.entity.marks.MarksSubject
import cz.lastaapps.bakalari.api.entity.subjects.Subject
import cz.lastaapps.bakalari.api.entity.subjects.Teacher
import cz.lastaapps.bakalari.api.entity.themes.Theme
import cz.lastaapps.bakalari.api.entity.timetable.Change
import cz.lastaapps.bakalari.api.entity.timetable.Day
import cz.lastaapps.bakalari.api.entity.timetable.Hour
import cz.lastaapps.bakalari.api.entity.timetable.Lesson
import cz.lastaapps.bakalari.api.entity.user.ModuleFeature
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.HashMap

/**Database for the API data*/
@Database(
    entities = [
        AbsenceDay::class, AbsenceSubject::class, ThresholdHolder::class,

        Attachment::class,

        EventHolder::class, EventTime::class, EventClassData::class,
        EventClassRelation::class, EventTeacherRelation::class, EventRoomRelation::class, EventStudentRelation::class,

        HomeworkHolder::class, HomeworkAttachmentRelation::class,

        Mark::class, MarksSubject::class,

        Subject::class, Teacher::class, Theme::class,

        Day::class, Hour::class, Lesson::class, Change::class, TimetableCycleRelation::class,
        LessonCycleRelation::class, LessonGroupRelation::class, LessonHomeworkRelation::class,

        UserHolder::class, ModuleFeature::class,

        JSONStorageRepository.JSONPair::class,

        ClassData::class, TeacherData::class, RoomData::class, StudentData::class,
        SubjectData::class, GroupData::class, ClassGroupData::class, CycleData::class
    ],
    version = 19,
    exportSchema = true,
)
@TypeConverters(APIBase.Converter::class)
abstract class APIBase : RoomDatabase() {

    companion object {

        private val TAG get() = APIBase::class.java.simpleName

        @Volatile
        private var instancesMap = HashMap<UUID, APIBase>()

        fun getDatabaseBlocking(context: Context, accountUUID: UUID): APIBase? = runBlocking {
            getDatabase(context, accountUUID)
        }

        suspend fun getDatabase(context: Context, accountUUID: UUID): APIBase? {
            val account = AccountsDatabase.getDatabase(context).repository
                .getByUUID(accountUUID) ?: return null

            return getDatabase(context, account)
        }

        fun getDatabase(context: Context, account: BakalariAccount): APIBase = synchronized(this) {

            val tempInstance = instancesMap[account.uuid]
            if (tempInstance != null) {
                return tempInstance
            }

            val name = createDatabaseName(account.uuid)
            val scope = CoroutineScope(Dispatchers.Default)

            val instance = Room.databaseBuilder(
                context.applicationContext,
                APIBase::class.java,
                name
            )
                .addCallback(APIBaseCallback(scope, name))
                //TODO database migration
                .fallbackToDestructiveMigration()
                .build().also {
                    it.databaseName = name
                    it.coroutineScope = scope
                    it.appContext = context.applicationContext
                    it.account = account
                }

            instancesMap[account.uuid] = instance
            return instance
        }

        suspend fun releaseDatabase(userId: UUID) {
            instancesMap.remove(userId)?.let {
                it.getScope().cancel()
                coroutineScope {
                    while (it.inTransaction()) yield()
                    it.close()
                }
            }
        }

        suspend fun deleteDatabase(context: Context, account: BakalariAccount) =
            deleteDatabase(context, account.uuid)

        suspend fun deleteDatabase(context: Context, accountId: UUID) {
            getDatabase(context, accountId)?.deleteAll()
            releaseDatabase(accountId)
            context.deleteDatabase(createDatabaseName(accountId))
        }

        private fun createDatabaseName(accountId: UUID) =
            APIBaseKeys.DATABASE_NAME + "_" + accountId
    }

    private lateinit var databaseName: String
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var appContext: Context
    private lateinit var account: BakalariAccount

    fun getAppContext() = appContext
    fun getAccount() = account
    fun getScope() = coroutineScope

    abstract fun absenceDao(): AbsenceDao
    abstract fun marksDao(): MarksDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun eventsDao(): EventsDao
    abstract fun subjectDao(): SubjectDao
    abstract fun themeDao(): ThemesDao
    abstract fun timetableDao(): TimetableDao
    abstract fun userDao(): UserDao

    val jsonStorageRepository by lazy { JSONStorageRepository(this) }

    abstract fun jsonStorageDao(): JSONStorageRepository.JSONStorageDao

    /** MUST be called when table is updated*/
    fun tablesUpdated(names: List<String>, time: ZonedDateTime = ZonedDateTime.now()) {
        appContext.getSharedPreferences(databaseName, Context.MODE_PRIVATE).edit().apply {
            for (name in names)
                putLong(name, time.toInstant().epochSecond)
        }.apply()
    }

    /** @return when was table last updated*/
    fun tablesLastUpdated(names: List<String>): Map<String, ZonedDateTime?> {
        val sp = appContext.getSharedPreferences(databaseName, Context.MODE_PRIVATE)
        val map = HashMap<String, ZonedDateTime?>()

        for (name in names) {
            val time = sp.getLong(name, 0)
            if (time == 0L) {
                map[name] = null
            } else {
                map[name] = ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), TimeTools.UTC)
            }
        }

        return map
    }

    fun deleteAll() = runBlocking {
        Log.i(TAG, "Deleting database data")

        appContext.getSharedPreferences(databaseName, Context.MODE_PRIVATE).edit()
            .clear().apply()

        clearAllTables()

        onDeleteActions.forEach { it() }

        jsonStorageRepository.deleteAll()
    }

    private val onDeleteActions = Collections.synchronizedList(mutableListOf<suspend () -> Unit>())
    fun addOnDeleteAction(action: suspend () -> Unit) = onDeleteActions.add(action)
    fun removeOnDeleteAction(action: suspend () -> Unit) = onDeleteActions.remove(action)

    override fun close() {
        onCloseActions.forEach { it() }

        super.close()
    }

    private val onCloseActions = Collections.synchronizedList(mutableListOf<() -> Unit>())
    fun addOnCloseAction(action: () -> Unit) = onCloseActions.add(action)
    fun removeOnCloseAction(action: () -> Unit) = onCloseActions.remove(action)


    private class APIBaseCallback(
        private val scope: CoroutineScope,
        private val databaseName: String
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Creating database $databaseName")

            (db as? APIBase)?.deleteAll()
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Opening database $databaseName")
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            Log.e(TAG, "Destructing database $databaseName")

            (db as? APIBase)?.deleteAll()
        }
    }

    class Converter {
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
        fun instantToLong(instant: Instant?) = instant?.epochSecond

        @TypeConverter
        fun longToInstant(long: Long?) = long?.let { Instant.ofEpochSecond(it) }

        @TypeConverter
        fun jsonToString(json: JSONObject?) = json?.toString() ?: "{}"

        @TypeConverter
        fun stringToJson(string: String?) = JSONObject(string ?: "{}")
    }
}