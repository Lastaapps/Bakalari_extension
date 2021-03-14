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

package cz.lastaapps.bakalari.app.ui.others

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.user.data.User

/** Show message to teacher accounts that they wouldn't be able to use all the features */
class TeacherWarning : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        Log.i(TAG, "Creating dialog")

        return AlertDialog.Builder(requireContext())
            .setPositiveButton(R.string.teacher_warning_button) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .setTitle(R.string.teacher_warning_title)
            .setMessage(R.string.teacher_warning_message)
            .create()
            .also {
                it.setOnShowListener {
                    shown(requireContext())
                }
            }
    }

    /** shows dialog with local class TAG*/
    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {
        private val TAG = TeacherWarning::class.java.simpleName
        private const val SP_KEY = "TEACHER_WARNING"
        private const val SP_SHOWN = "SHOWN"

        /**if dialog should be shown*/
        fun shouldShow(context: Context, user: User): Boolean {
            val shown = context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getBoolean(SP_SHOWN, false)
            val isTeacher = user.userType == User.ROLE_TEACHER
                    || user.userType == User.ROLE_HEADMASTERSHIP
            return !shown && isTeacher
        }

        /**@return if dialog was already shown*/
        fun shown(context: Context, state: Boolean = true) {
            context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(SP_SHOWN, state)
                .apply()
        }
    }
}