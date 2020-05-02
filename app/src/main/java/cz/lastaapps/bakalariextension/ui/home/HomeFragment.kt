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

package cz.lastaapps.bakalariextension.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.ui.marks.NewMarksFragment
import cz.lastaapps.bakalariextension.ui.timetable.small.SmallTimetableFragment
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

class HomeFragment : Fragment() {

    companion object {
        private val TAG = HomeFragment::class.java.simpleName

        //tag (~= id) of small timetable fragment
        private const val TIMETABLE_SMALL_FRAGMENT_TAG = "TIMETABLE_SMALL_FRAGMENT_TAG"
        private const val NEW_MARKS_FRAGMENT_TAG = "NEW_MARKS_FRAGMENT_TAG"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating HomeFragment")

        //inflates layout
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val nameView = root.findViewById<TextView>(R.id.name)
        val typeView = root.findViewById<TextView>(R.id.type)
        val schoolView = root.findViewById<TextView>(R.id.school)

        //sets up info about user
        nameView.text = User.get(User.NAME)
        typeView.text = User.getClassAndRole()
        schoolView.text = User.get(User.SCHOOL)

        //layout for oder widgets
        fragmentContainer = root.findViewById(R.id.fragment_container)

        return root
    }

    override fun onStart() {
        super.onStart()

        //adds fragments
        fragments()
    }

    /**Layout in HomeFragment containing all oder Fragments like small timetable*/
    private lateinit var fragmentContainer: LinearLayout

    /***/
    private fun fragments() {
        //first append of Fragments
        val fragmentTransaction = childFragmentManager.beginTransaction()

        //checks if fragment wasn't added before
        if (childFragmentManager.findFragmentByTag(NEW_MARKS_FRAGMENT_TAG) == null) {

            val frag = NewMarksFragment()
            //adds fragment to layout
            fragmentTransaction.add(R.id.fragment_container, frag, NEW_MARKS_FRAGMENT_TAG)
            //animation insert - not working
            fragmentTransaction.setCustomAnimations(
                R.anim.home_fragments,
                android.R.anim.fade_out
            )
        }

        if (LocalDate.now().dayOfWeek.value in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value) {
            //checks if fragment wasn't added before
            if (childFragmentManager.findFragmentByTag(TIMETABLE_SMALL_FRAGMENT_TAG) == null) {
                //inits viewModel for the fragment
                val model: SmallTimetableFragment.STViewModel by viewModels()
                val frag = SmallTimetableFragment()
                //adds fragment to layout
                fragmentTransaction.add(R.id.fragment_container, frag, TIMETABLE_SMALL_FRAGMENT_TAG)
                //animation insert - not working
                fragmentTransaction.setCustomAnimations(
                    R.anim.home_fragments,
                    android.R.anim.fade_out
                )
            }
        }

        //finishes adding fragments, updates screen
        fragmentTransaction.commit()
    }
}