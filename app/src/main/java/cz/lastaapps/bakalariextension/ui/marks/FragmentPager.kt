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

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.marks.predictor.PredictorFragment

/**Changes fragments for tab selection*/
class FragmentPager(val context: Context, fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = Array<Fragment?>(3) { null }

    /**@return mark Fragment by position*/
    override fun getItem(position: Int): Fragment {
        //holds fragments in memory for faster swiping
        if (fragments[position] == null)
            fragments[position] = when (position) {
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

        return fragments[position]!!
    }

    /**@return label for tab by position*/
    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(
            when (position) {
                0 -> R.string.marks_by_date
                1 -> R.string.marks_by_subject
                else -> R.string.marks_predictor
            }
        )
    }

    /**@return 3*/
    override fun getCount(): Int {
        return 3
    }

    /**@return position as id*/
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}