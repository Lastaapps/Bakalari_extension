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

package cz.lastaapps.bakalari.app.ui.navigation.navview

import android.view.MenuItem
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.google.android.material.navigation.NavigationView
import cz.lastaapps.bakalari.api.entity.user.User
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.uitools.observeForControllerGraphChanges
import cz.lastaapps.bakalari.app.ui.user.UserViewModel

class ItemsController(fragmentActivity: FragmentActivity) {

    private val activity = fragmentActivity as MainActivity
    private val navView = activity.findViewById<NavigationView>(R.id.nav_view)
    private val menu = navView.menu

    private var userViewModel: UserViewModel? = null

    init {
        observeForControllerGraphChanges(
            activity, {
                createViewModel()
                userViewModel!!.data.observe({ activity.lifecycle }) {
                    initMenu(it)
                }
            }, {}, {
                clear()
                userViewModel?.data?.removeObservers { activity.lifecycle }
                userViewModel = null
            }
        )
    }

    private fun createViewModel() {
        if (userViewModel == null) {
            val v: UserViewModel by activity.accountsViewModels()
            userViewModel = v
        }
    }

    private val itemList = listOf(
        Data(null, R.id.nav_home, R.string.menu_home, R.drawable.nav_home),
        Data(
            User.TIMETABLE,
            R.id.nav_timetable,
            R.string.menu_timetable,
            R.drawable.nav_timetable,
        ),
        Data(User.MARKS, R.id.nav_marks, R.string.menu_marks, R.drawable.nav_marks),
        Data(User.HOMEWORK, R.id.nav_homework, R.string.menu_homework, R.drawable.nav_homework),
        Data(User.EVENTS, R.id.nav_events, R.string.menu_events, R.drawable.nav_events),
        Data(User.ABSENCE, R.id.nav_absence, R.string.menu_absence, R.drawable.nav_absence),
        Data(
            User.SUBJECTS,
            R.id.nav_teacher_list,
            R.string.menu_teacher_list,
            R.drawable.nav_teacher,
        ),
        Data(
            User.SUBJECTS,
            R.id.nav_subject_list,
            R.string.menu_subject_list,
            R.drawable.nav_subject,
        ),
    )

    private fun initMenu(user: User) {
        for (item in itemList) {
            addItem(user, item)
        }
    }

    private var order = 0
    private fun addItem(
        user: User,
        module: String?,
        @IdRes id: Int,
        @StringRes text: Int,
        @DrawableRes icon: Int
    ) {
        if (module == null || user.isModuleEnabled(module)) {
            if (menu.findItem(id) == null) {
                menu.add(R.id.nav_items_group, id, order, text).setIcon(icon)
            }
        }
    }

    private fun addItem(user: User, data: Data) = data.run { addItem(user, module, id, text, icon) }

    fun clear() {
        menu.removeGroup(R.id.nav_items_group)
    }

    fun onNavigationItemSelected(item: MenuItem): Boolean {

        val controller = activity.findNavController()
        when (item.itemId) {
            R.id.nav_home -> {
                controller.navigate(R.id.nav_home)
            }
            R.id.nav_timetable -> {
                controller.navigate(R.id.nav_timetable)
            }
            R.id.nav_marks -> {
                controller.navigate(R.id.nav_marks)
            }
            R.id.nav_homework -> {
                controller.navigate(R.id.nav_homework)
            }
            R.id.nav_absence -> {
                controller.navigate(R.id.nav_absence)
            }
            R.id.nav_teacher_list -> {
                controller.navigate(R.id.nav_teacher_list)
            }
            R.id.nav_subject_list -> {
                controller.navigate(R.id.nav_subject_list)
            }

            else -> return false
        }

        return true
    }

    data class Data(
        val module: String?,
        @IdRes val id: Int,
        @StringRes val text: Int,
        @DrawableRes val icon: Int
    )
}