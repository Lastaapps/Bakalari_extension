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

package cz.lastaapps.bakalari.api.entity.user

import android.os.Parcelable
import androidx.room.Entity
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import kotlinx.parcelize.Parcelize

typealias ModuleList = ArrayList<ModuleFeature>

@Parcelize
@Entity(tableName = APIBaseKeys.USER_MODULE, primaryKeys = ["userId", "moduleName", "featureName"])
data class ModuleFeature(
    val userId: String,
    val moduleName: String,
    val featureName: String,
) : Comparable<ModuleFeature>, Parcelable {
    override fun compareTo(other: ModuleFeature): Int {
        val idComp = userId.compareTo(other.userId)
        if (idComp != 0) return idComp

        val modCom = moduleName.compareTo(other.moduleName)
        if (modCom != 0) return modCom

        return featureName.compareTo(other.featureName)
    }
}