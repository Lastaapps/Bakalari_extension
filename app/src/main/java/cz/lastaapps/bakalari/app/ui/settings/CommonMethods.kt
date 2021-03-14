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

package cz.lastaapps.bakalari.app.ui.settings

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cz.lastaapps.bakalari.app.MainActivity

/**sets visibility of preference*/
fun PreferenceFragmentCompat.prefVisibility(key: String, visible: Boolean): Preference {
    fp(key)?.also {
        it.isVisible = visible

        return it
    }
    throw IllegalArgumentException("Preference not found")
}

/**changes visibility of the preference and sets onChange listener*/
fun PreferenceFragmentCompat.prefChange(
    key: String,
    visible: Boolean,
    todo: ((Preference, Any) -> Boolean)
): Preference {
    fp(key)?.also {
        if (visible) {
            it.isVisible = true
            it.setOnPreferenceChangeListener { preference, newValue ->
                todo(preference, newValue)
            }
        } else {
            it.isVisible = false
        }

        return it
    }
    throw IllegalArgumentException("Preference not found")
}

/**changes visibility of the preference and sets onClick listener*/
fun PreferenceFragmentCompat.prefClick(
    key: String,
    visible: Boolean,
    todo: ((Preference) -> Boolean)
): Preference {
    fp(key)?.also {
        if (visible) {
            it.isVisible = true
            it.setOnPreferenceClickListener { preference ->
                todo(preference)
            }
        } else {
            it.isVisible = false
        }

        return it
    }
    throw IllegalArgumentException("Preference not found")
}

/**
 * Represents findPreference<Preference>
 */
fun PreferenceFragmentCompat.fp(key: String): Preference? = findPreference(key)


/**restarts activity*/
private fun Fragment.restartActivity() {
    requireActivity().finish()
    startActivity(Intent(requireActivity(), MainActivity::class.java))
}

/**Kills process and restarts it*/
private fun Fragment.restartProcess() {

    Log.i("CommonMethods", "Killing app process")

    //restarts app
    /*val intent = Intent(this, LoadingActivity::class.java)
    val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    mgr[AlarmManager.RTC, System.currentTimeMillis() + 1000] = PendingIntent.getActivity(
        this.baseContext,
        0,
        intent,
        intent.flags
    )*/
    //restarts app
    Handler(Looper.getMainLooper()).postDelayed(
        {
            Process.killProcess(Process.myPid())
        }, 50
    )
}

