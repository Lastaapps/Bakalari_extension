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

package cz.lastaapps.bakalariextension.ui.marks

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.marks.predictor.PredictorFragment

/**Changes fragments for tab selection*/
class MarksPager(val fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**@return mark Fragment by position*/
    override fun createFragment(position: Int): Fragment {
        //holds fragments in memory for faster swiping
        return when (position) {
            0 -> {
                ByDateFragment()
            }
            1 -> {
                BySubjectFragment()
            }
            else -> {
                PredictorFragment()
            }
        }
    }

    /**@return label for tab by position*/
    fun getPageTitle(position: Int): CharSequence? {
        return fragment.requireContext().getString(
            when (position) {
                0 -> R.string.marks_by_date
                1 -> R.string.marks_by_subject
                else -> R.string.marks_predictor
            }
        )
    }

    /**@return 3*/
    override fun getItemCount(): Int {
        return 3
    }

    /**@return position as id*/
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}