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

package cz.lastaapps.bakalari.app.ui.user.absence

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cz.lastaapps.bakalari.app.R

class AbsencePager(private val fragment: Fragment, private val showSubjects: Boolean) :
    FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {

        return when (position + if (!showSubjects) 1 else 0 /*skips index 0*/) {
            0 -> AbsenceSubjectFragment()
            1 -> AbsenceDayFragment()
            else -> AbsenceMonthFragment()
        }
    }

    /**Days and month is shown always, subjects require permission*/
    override fun getItemCount(): Int {
        return 2 + if (showSubjects) 1 else 0
    }

    /**title shown in the top TabLayout*/
    fun getPageTitle(position: Int): String {
        return fragment.requireContext().getString(
            when (position + if (!showSubjects) 1 else 0 /*skips index 0*/) {
                0 -> R.string.absence_title_subjects
                1 -> R.string.absence_title_days
                else -> R.string.absence_title_months
            }
        )
    }

    override fun getItemId(position: Int): Long {
        return (position + if (!showSubjects) 1 else 0).toLong()
    }
}