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

package cz.lastaapps.bakalari.api.entity.core

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_CLASS,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class ClassData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable

@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_TEACHER,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class TeacherData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable

@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_ROOM,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class RoomData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable

@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_STUDENT,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class StudentData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable

@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_SUBJECT,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class SubjectData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable

@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_GROUP,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class GroupData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable

@Parcelize
@Entity(
    tableName = APIBaseKeys.DATA_CYCLE,
    primaryKeys = ["data_id"],
    indices = [Index(value = ["data_id"])]
)
data class CycleData(@Embedded(prefix = "data_") val data: SimpleData) : Parcelable