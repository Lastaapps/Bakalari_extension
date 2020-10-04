/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalariextension.api.events.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.lastaapps.bakalariextension.api.database.APIBase
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Entity(tableName = APIBase.EVENTS_TIMES)
data class EventTime(
    @ColumnInfo(name = "data_id", index = true)
    var eventId: String,
    var wholeDay: Boolean,
    var start: ZonedDateTime,
    var end: ZonedDateTime,
    @PrimaryKey(autoGenerate = true)
    var autokey: Int? = null
) : Parcelable