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

package cz.lastaapps.bakalariextension.ui.absence

import android.content.Context
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.absence.AbsenceLoader
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceRoot
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel

class AbsenceViewModel : RefreshableViewModel<AbsenceRoot>(TAG) {

    companion object {
        private val TAG = AbsenceViewModel::class.java.simpleName
    }

    override suspend fun loadServer(): AbsenceRoot? {
        return AbsenceLoader.loadFromServer()
    }

    override suspend fun loadStorage(): AbsenceRoot? {
        return AbsenceLoader.loadFromStorage()
    }

    override fun shouldReload(): Boolean {
        return AbsenceLoader.shouldReload()
    }

    override fun isEmpty(data: AbsenceRoot): Boolean {
        return data.days.isEmpty()
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.absence_no_absence)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.absence_failed_to_load)
    }
}