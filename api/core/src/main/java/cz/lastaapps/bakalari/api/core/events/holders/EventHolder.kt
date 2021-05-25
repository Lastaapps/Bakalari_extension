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

package cz.lastaapps.bakalari.api.core.events.holders

import androidx.room.*
import cz.lastaapps.bakalari.api.core.DataIdList
import cz.lastaapps.bakalari.api.core.SimpleData
import cz.lastaapps.bakalari.api.core.database.APIBase
import cz.lastaapps.bakalari.api.core.database.RoomData
import cz.lastaapps.bakalari.api.core.database.StudentData
import cz.lastaapps.bakalari.api.core.database.TeacherData
import java.time.ZonedDateTime

@Entity(tableName = APIBase.EVENTS)
data class EventHolder(
    @PrimaryKey
    @ColumnInfo(index = true)
    val id: String,
    val group: Int = 0,
    val title: String,
    val description: String,
    val eventStart: ZonedDateTime,
    val eventEnd: ZonedDateTime,
    @Embedded(prefix = "type_")
    val type: SimpleData,
    val note: String?,
    val dateChanged: ZonedDateTime
)

data class EventHolderWithLists(
    @Embedded val holder: EventHolder,
    @Relation(
        parentColumn = "id",
        entityColumn = "data_id",
    )
    val times: List<EventTime>,
    @Relation(
        parentColumn = "id",
        entityColumn = "data_id",
        associateBy = Junction(EventClassRelation::class)
    )
    val classes: List<EventClassData>,
    @Relation(
        parentColumn = "id",
        entityColumn = "data_id",
        associateBy = Junction(EventTeacherRelation::class)
    )
    val teachers: List<TeacherData>,
    @Relation(
        parentColumn = "id",
        entityColumn = "data_id",
        associateBy = Junction(EventRoomRelation::class)
    )
    val rooms: List<RoomData>,
    @Relation(
        parentColumn = "id",
        entityColumn = "data_id",
        associateBy = Junction(EventStudentRelation::class)
    )
    val students: List<StudentData>,
) {
    fun toEvent(): Event = holder.run {
        Event(
            id,
            group,
            title,
            description,
            ArrayList(times),
            eventStart,
            eventEnd,
            type,
            DataIdList(classes.map { it.data }),
            //Any(),
            DataIdList(teachers.map { it.data }),
            //Any(),
            DataIdList(rooms.map { it.data }),
            //Any(),
            DataIdList(students.map { it.data }),
            note,
            dateChanged
        )
    }

    companion object {
        fun fromEvent(event: Event): EventHolderWithLists = event.run {
            EventHolderWithLists(
                EventHolder(
                    id, group, title, description, eventStart, eventEnd, type, note, dateChanged
                ),
                times,
                classes.map { EventClassData(it) },
                teachers.map { TeacherData(it) },
                rooms.map { RoomData(it) },
                students.map { StudentData(it) },
            )
        }
    }
}

@Entity(
    tableName = APIBase.EVENTS_CLASSES_DATA,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class EventClassData(@Embedded(prefix = "data_") val data: SimpleData)

@Entity(primaryKeys = ["id", "data_id"], tableName = APIBase.EVENTS_CLASSES_RELATIONS)
data class EventClassRelation(
    @ColumnInfo(index = true) val id: String,
    @ColumnInfo(name = "data_id", index = true) val dataId: String
)

@Entity(primaryKeys = ["id", "data_id"], tableName = APIBase.EVENTS_TEACHES)
data class EventTeacherRelation(
    @ColumnInfo(index = true) val id: String,
    @ColumnInfo(name = "data_id", index = true) val dataId: String
)

@Entity(primaryKeys = ["id", "data_id"], tableName = APIBase.EVENTS_ROOMS)
data class EventRoomRelation(
    @ColumnInfo(index = true) val id: String,
    @ColumnInfo(name = "data_id", index = true) val dataId: String
)

@Entity(primaryKeys = ["id", "data_id"], tableName = APIBase.EVENTS_STUDENTS)
data class EventStudentRelation(
    @ColumnInfo(index = true) val id: String,
    @ColumnInfo(name = "data_id", index = true) val dataId: String
)

