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

package cz.lastaapps.bakalariextension.ui.timetable.normal

import androidx.lifecycle.ViewModel
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.Settings
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime

/**Contains data for */
class TimetableViewModel : ViewModel() {
    //current date for actual timetable
    var dateTime: ZonedDateTime =
        TimeTools.monday.plusDays(
            Settings(App.context).getTimetableDayOffset().toLong()
        )

    //if permanent timetable is shown now
    var isPermanent = false

    //cycle index for permanent timetable
    var cycleIndex = 0

    //last loaded week
    var week: Week? = null
}