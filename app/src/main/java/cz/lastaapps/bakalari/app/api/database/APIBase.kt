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

package cz.lastaapps.bakalari.app.api.database

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.lastaapps.bakalari.app.api.absence.AbsenceDao
import cz.lastaapps.bakalari.app.api.absence.AbsenceRepository
import cz.lastaapps.bakalari.app.api.absence.data.AbsenceDay
import cz.lastaapps.bakalari.app.api.absence.data.AbsenceSubject
import cz.lastaapps.bakalari.app.api.absence.data.ThresholdHolder
import cz.lastaapps.bakalari.app.api.attachment.data.Attachment
import cz.lastaapps.bakalari.app.api.events.EventsDao
import cz.lastaapps.bakalari.app.api.events.EventsRepository
import cz.lastaapps.bakalari.app.api.events.data.*
import cz.lastaapps.bakalari.app.api.homework.HomeworkDao
import cz.lastaapps.bakalari.app.api.homework.HomeworkRepository
import cz.lastaapps.bakalari.app.api.homework.data.HomeworkAttachmentRelation
import cz.lastaapps.bakalari.app.api.homework.data.HomeworkHolder
import cz.lastaapps.bakalari.app.api.marks.MarksDao
import cz.lastaapps.bakalari.app.api.marks.MarksRepository
import cz.lastaapps.bakalari.app.api.marks.data.Mark
import cz.lastaapps.bakalari.app.api.marks.data.MarksSubject
import cz.lastaapps.bakalari.app.api.subjects.SubjectDao
import cz.lastaapps.bakalari.app.api.subjects.SubjectRepository
import cz.lastaapps.bakalari.app.api.subjects.data.Subject
import cz.lastaapps.bakalari.app.api.subjects.data.Teacher
import cz.lastaapps.bakalari.app.api.themes.ThemesDao
import cz.lastaapps.bakalari.app.api.themes.ThemesMainRepository
import cz.lastaapps.bakalari.app.api.themes.data.Theme
import cz.lastaapps.bakalari.app.api.timetable.TimetableDao
import cz.lastaapps.bakalari.app.api.timetable.TimetableMainRepository
import cz.lastaapps.bakalari.app.api.timetable.data.*
import cz.lastaapps.bakalari.app.api.user.UserDao
import cz.lastaapps.bakalari.app.api.user.UserRepository
import cz.lastaapps.bakalari.app.api.user.data.ModuleFeature
import cz.lastaapps.bakalari.app.api.user.data.UserHolder
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.App
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
    //TODO after database is ready, set to true, maybe...
    exportSchema = false
)
@TypeConverters(APIBase.Converter::class)
abstract class APIBase : RoomDatabase() {

    companion object {

        private val TAG = APIBase::class.java.simpleName

        private const val DATABASE_NAME = "API_DATABASE"

        //table names
        const val ABSENCE_THRESHOLD = "absence_threshold"
        const val ABSENCE_DAY = "absence_day"
        const val ABSENCE_SUBJECT = "absence_subject"

        const val ATTACHMENT = "attachments"

        const val EVENTS = "events"
        const val EVENTS_TIMES = "events_times"
        const val EVENTS_CLASSES_DATA = "events_classes"
        const val EVENTS_CLASSES_RELATIONS = "rel_events_classes"
        const val EVENTS_TEACHES = "rel_events_teachers"
        const val EVENTS_ROOMS = "rel_events_rooms"
        const val EVENTS_STUDENTS = "rel_events_students"

        const val HOMEWORK = "homework"
        const val HOMEWORK_ATTACHMENTS = "rel_homework_attachments"

        const val MARK_SUBJECT = "mark_subjects"
        const val MARKS = "marks"

        const val SUBJECTS = "subjects"
        const val TEACHERS = "teachers"
        const val THEMES = "themes"

        const val TIMETABLE_CHANGE = "timetable_changes"
        const val TIMETABLE_DAY = "timetable_days"
        const val TIMETABLE_HOUR = "timetable_hours"
        const val TIMETABLE_LESSON = "timetable_lessons"
        const val TIMETABLE_CYCLE_RELATION = "rel_timetable_cycle"
        const val TIMETABLE_LESSON_CYCLE = "rel_timetable_lessons_cycle"
        const val TIMETABLE_LESSON_GROUP = "rel_timetable_lessons_group"
        const val TIMETABLE_LESSON_HOMEWORK = "rel_timetable_lessons_homework"

        const val USER = "user"
        const val USER_MODULE = "user_modules"

        const val JSON_STORAGE = "json_storage"

        const val DATA_CLASS = "data_classes"
        const val DATA_TEACHER = "data_teachers"
        const val DATA_ROOM = "data_rooms"
        const val DATA_STUDENT = "data_students"
        const val DATA_SUBJECT = "data_subject"
        const val DATA_GROUP = "data_groups"
        const val DATA_CLASS_GROUP = "data_class_groups"
        const val DATA_CYCLE = "data_cycles"

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

        private fun createDatabaseName(accountId: UUID) = DATABASE_NAME + "_" + accountId
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

    val absenceRepository by lazy { AbsenceRepository(this) }
    val marksRepository by lazy { MarksRepository(this) }
    val homeworkRepository by lazy { HomeworkRepository(this) }
    val eventsRepository by lazy { EventsRepository(this) }
    val subjectTeacherRepository by lazy { SubjectRepository(this) }
    val themesRepository by lazy { ThemesMainRepository(this) }
    val timetableRepository by lazy { TimetableMainRepository(this) }
    val userRepository by lazy { UserRepository(this) }
    val jsonStorageRepository by lazy { JSONStorageRepository(this) }

    abstract fun jsonStorageDao(): JSONStorageRepository.JSONStorageDao

    /** MUST be called when table is updated*/
    fun tablesUpdated(names: List<String>, time: ZonedDateTime = ZonedDateTime.now()) {
        App.context.getSharedPreferences(databaseName, Context.MODE_PRIVATE).edit().apply {
            for (name in names)
                putLong(name, time.toInstant().epochSecond)
        }.apply()
    }

    /** @return when was table last updated*/
    fun tablesLastUpdated(names: List<String>): Map<String, ZonedDateTime?> {
        val sp = App.context.getSharedPreferences(databaseName, Context.MODE_PRIVATE)
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

        App.context.getSharedPreferences(databaseName, Context.MODE_PRIVATE).edit()
            .clear().apply()

        clearAllTables()

        absenceRepository.deleteAll()
        eventsRepository.deleteAll()
        marksRepository.deleteAll()
        subjectTeacherRepository.deleteAll()
        themesRepository.deleteAll()

        jsonStorageRepository.deleteAll()
    }

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