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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.api.web.LoginToken
import cz.lastaapps.bakalariextension.databinding.FragmentHomeBinding
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.absence.AbsenceOverviewFragment
import cz.lastaapps.bakalariextension.ui.events.EventsUpcomingFragment
import cz.lastaapps.bakalariextension.ui.homework.HmwOverview
import cz.lastaapps.bakalariextension.ui.marks.NewMarksFragment
import cz.lastaapps.bakalariextension.ui.timetable.small.SmallTimetableFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }

    private lateinit var binding: FragmentHomeBinding
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        requireActivity().actionBar?.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating HomeFragment")

        //inflates layout
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)


        userViewModel.runOrRefresh(lifecycle) {
            binding.apply {

                //shows up info about user in the top
                name.text = it.normalFunName
                type.text = it.getClassAndRole()
                school.text = it.schoolName
            }
        }

        addFragments()

        return binding.root
    }

    /** Adds fragments to home fragment if theirs module is available*/
    private fun addFragments() {

        val user = userViewModel.requireData()
        val transaction = childFragmentManager.beginTransaction()

        if (user.isModuleEnabled(User.TIMETABLE)) {
            transaction.add(R.id.timetable_fragment, SmallTimetableFragment())
        }

        if (user.isModuleEnabled(User.MARKS)) {
            transaction.add(R.id.marks_fragment, NewMarksFragment())
        }

        if (user.isModuleEnabled(User.HOMEWORK)) {
            transaction.add(R.id.homework_fragment, HmwOverview())
        }

        if (user.isModuleEnabled(User.EVENTS)) {
            transaction.add(R.id.events_fragment, EventsUpcomingFragment())
        }

        if (user.isModuleEnabled(User.ABSENCE)) {
            transaction.add(R.id.absence_fragment, AbsenceOverviewFragment())
        }

        transaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.web_app_bar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_web_module) {

            lifecycleScope.launch(Dispatchers.IO) {

                val url = LoginToken.loginUrl()

                withContext(Dispatchers.Main) {
                    if (url != null) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        requireContext().startActivity(browserIntent)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.web_module_no_internet,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            true
        } else
            super.onOptionsItemSelected(item)
    }
}