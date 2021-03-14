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

package cz.lastaapps.bakalari.app.api.user.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import cz.lastaapps.bakalari.app.api.SimpleData
import cz.lastaapps.bakalari.app.api.database.APIBase

@Entity(tableName = APIBase.USER)
data class UserHolder(
    @PrimaryKey
    val uid: String,
    val campaignCategory: String,
    @Embedded(prefix = "class_")
    val classData: SimpleData,
    val fullName: String,
    val schoolName: String,
    val schoolType: String,
    val userType: String,
    val userTypeText: String,
    val studyYear: Int,
    @Embedded(prefix = "semester_")
    val semester: Semester?
) {
    companion object {
        fun fromUser(user: User): UserHolder = user.run {
            UserHolder(
                uid,
                campaignCategory,
                classInfo,
                fullName,
                schoolName,
                schoolType,
                userType,
                userTypeText,
                studyYear,
                semester
            )
        }
    }
}

data class UserHolderWithLists(
    @Embedded
    val holder: UserHolder,
    @Relation(
        parentColumn = "uid",
        entityColumn = "userId"
    )
    val modulesFeatures: List<ModuleFeature>
) {
    fun toUser(): User = holder.run {
        User(
            uid,
            campaignCategory,
            classData,
            fullName,
            schoolName,
            schoolType,
            userType,
            userTypeText,
            studyYear,
            ModuleList(modulesFeatures),
            semester
        )
    }
}

