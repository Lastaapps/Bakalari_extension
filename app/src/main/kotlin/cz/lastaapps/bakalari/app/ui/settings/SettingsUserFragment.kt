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
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cz.lastaapps.bakalari.api.entity.user.User
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.others.LicenseDialog
import cz.lastaapps.bakalari.app.ui.others.WhatsNew
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.uitools.settingsViewModels
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import cz.lastaapps.bakalari.settings.R as SettR

/**
 * Manages settings UI, actual settings are saved in *.tools.Settings
 * Fragment with all the settings
 */
class SettingsUserFragment : PreferenceFragmentCompat() {

    companion object {
        private val TAG get() = SettingsUserFragment::class.java.simpleName
    }

    private val viewModel: SettingsViewModel by settingsViewModels()
    private val userViewModel: UserViewModel by accountsViewModels()
    private lateinit var sett: MySettings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        Log.i(TAG, "Creating preferences")

        //reference to actual Settings
        sett = MySettings.withAppContext()

        //user can be null if user opens settings before login
        val user = userViewModel.data.value

        sett.initSettings()
        setPreferencesFromResource(R.xml.settings_root, rootKey)

        //sets actions for all the actions

        //TIMETABLE
        prefVisibility(sett.TIMETABLE_CATEGORY, user?.isModuleEnabled(User.TIMETABLE) ?: false)

        //from which day should be next week shown
        prefChange(
            sett.TIMETABLE_DAY,
            user?.isFeatureEnabled(User.TIMETABLE_SHOW) ?: false
        ) { _, newValue ->
            Log.i(TAG, "Show new timetable day changed to $newValue")
            true
        }

        //from which day should be next week shown
        prefChange(
            sett.TIMETABLE_PREVIEW,
            user?.isFeatureEnabled(User.TIMETABLE_SHOW) ?: false
        ) { _, newValue ->
            Log.i(TAG, "Show preview for tomorrow day changed to $newValue")
            true
        }


        //MARKS
        prefVisibility(sett.MARKS_CATEGORY, user?.isModuleEnabled(User.MARKS) ?: false)

        //for how long is a mark shown as new
        prefVisibility(sett.MARKS_SHOW_NEW, user?.isModuleEnabled(User.MARKS) ?: false)


        //EVENTS
        prefVisibility(sett.EVENTS_CATEGORY, user?.isModuleEnabled(User.EVENTS) ?: false)

        //for showing events for the next days
        prefChange(
            sett.EVENTS_SHOW_FOR_DAY,
            user?.isFeatureEnabled(User.EVENTS_SHOW) ?: false
        ) { _, newValue ->
            Log.i(TAG, "Show events for the next day changed to $newValue")

            if (user?.isModuleEnabled(User.TIMETABLE) != true) {
                val array = resources.getStringArray(SettR.array.sett_events_show_for_day)

                if (array.indexOf(newValue) in 1..4) {

                    Toast.makeText(
                        requireContext(),
                        SettR.string.sett_events_show_for_day_disabled,
                        Toast.LENGTH_LONG
                    ).show()

                    return@prefChange false
                }
            }

            true
        }


        //ANALYTICS
        //if user's town and school can be send in analytics and reports
        prefChange(sett.SEND_TOWN_SCHOOL, true) { _, newValue ->
            Log.i(TAG, "Send town and school changed to $newValue")
            true
        }


        //OTHER
        //resets settings
        prefClick(sett.RESET, true) { _ ->
            Log.i(TAG, "!!! Resetting settings !!!")

            sett.getSP().edit().clear().apply()
            sett.initSettings()

            preferenceScreen = null
            addPreferencesFromResource(R.xml.settings_root)

            viewModel.relaunchApp = true

            true
        }

        //shows whats new
        prefClick(sett.SHOW_WHATS_NEW, true) { _ ->
            Log.i(TAG, "Showing What's new")

            WhatsNew(requireContext()).showDialog()

            true
        }

        //shows license
        prefClick(sett.ABOUT, true) { _ ->
            Log.i(TAG, "Showing about")

            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.nav_about)

            true
        }

        //shows license
        prefClick(sett.LICENSE, true) { _ ->
            Log.i(TAG, "Showing license")

            LicenseDialog.viewLicense(requireActivity())

            true
        }

    }

    /**sets visibility of preference*/
    private fun prefVisibility(key: String, visible: Boolean) {
        fp(key)?.also {
            it.isVisible = visible
        }
    }

    /**changes visibility of the preference and sets onChange listener*/
    private fun prefChange(key: String, visible: Boolean, todo: ((Preference, Any) -> Boolean)) {
        fp(key)?.also {
            if (visible) {
                it.setOnPreferenceChangeListener { preference, newValue ->
                    todo(preference, newValue)
                }
            } else {
                it.isVisible = false
            }
        }
    }

    /**changes visibility of the preference and sets onClick listener*/
    private fun prefClick(key: String, visible: Boolean, todo: ((Preference) -> Boolean)) {
        fp(key)?.also {
            if (visible) {
                it.setOnPreferenceClickListener { preference ->
                    todo(preference)
                }
            } else {
                it.isVisible = false
            }
        }
    }

    /**restarts activity*/
    private fun restartActivity() {
        requireActivity().finish()
        startActivity(Intent(requireActivity(), MainActivity::class.java))
    }

    /**Kills process and restarts it*/
    private fun restartProcess() {

        Log.i(TAG, "Killing app process")

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

    /**
     * Represents findPreference<Preference>
     */
    private fun fp(key: String): Preference? = findPreference(key)

    override fun onResume() {
        super.onResume()

        //if location was changed by oder activity
        fp(sett.DOWNLOAD_LOCATION)?.let {
            setDownloadLocationText(it)
        }
    }

    /**Sets readable text instead of content Uri on Q+
     * replaces*/
    private fun setDownloadLocationText(preference: Preference) {

        var result: String? = ""
        val stringUri = Uri.decode(sett.getDownloadLocation())

        //backup solution
        if (stringUri != "") {
            val index = {
                var toReturn = -1
                var spotted = 0
                for (i in stringUri.indices) {
                    val c = stringUri[i]
                    if (c == '/') {
                        if (spotted < 2)
                            spotted++
                        else {
                            toReturn = i
                            break
                        }
                    }
                }

                toReturn
            }.invoke()
            result = stringUri.substring(index)
        }

        val uri = Uri.parse(stringUri)
        result = uri.lastPathSegment ?: result

        preference.summary = result ?: ""
    }

}
