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

package cz.lastaapps.bakalari.app.ui.start.version

import android.content.Context
import android.content.SharedPreferences

object VersionChecker {

    private const val SP_NAME = "UPDATE_CHECKER"
    private const val VERSION_KEY = "VERSION"

    fun isUpdateRequired(context: Context): Boolean {
        return currentVersion > getSP(context).getInt(VERSION_KEY, currentVersion)
    }

    fun updated(context: Context) {
        getSP(context).edit().putInt(VERSION_KEY, currentVersion).apply()
    }

    //TODO version system
    private val currentVersion: Int = 0

    private fun getSP(context: Context): SharedPreferences =
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

}