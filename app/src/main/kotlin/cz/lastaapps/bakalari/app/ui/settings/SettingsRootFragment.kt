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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.others.LicenseDialog
import cz.lastaapps.bakalari.app.ui.others.WhatsNew
import cz.lastaapps.bakalari.app.ui.uitools.settingsViewModels
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings

class SettingsRootFragment : PreferenceFragmentCompat() {

    companion object {
        private val TAG get() = SettingsRootFragment::class.java.simpleName
    }

    private val viewModel: SettingsViewModel by settingsViewModels()
    private lateinit var sett: MySettings

    override fun onDestroy() {
        super.onDestroy()

        //TODO
        /*if (viewModel.relaunchApp) {
            if (!requireActivity().isChangingConfigurations) {
                //notifies that backup should be made
                BackupManager.dataChanged(App.context.packageName)

                restartProcess()
            }
        }*/
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        Log.i(TAG, "Creating preferences")

        //reference to actual Settings
        sett = MySettings.withAppContext()

        sett.initSettings()
        setPreferencesFromResource(R.xml.settings_root, rootKey)


        prefClick(sett.GENERAL, true) {
            findNavController().navigate(R.id.nav_settings_general)
            true
        }
        prefClick(sett.DATA_COLLECTION, true) {
            findNavController().navigate(R.id.nav_settings_data_collection)
            true
        }

        //navigation controller required, but only accessible after activities onCreate() call
        //onCreatePreference() can be called from Activity.onCreate(),
        //so preference is temporally hidden
        prefVisibility(sett.USER, false)

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

    @SuppressLint("RestrictedApi")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        var userDataExists = false
        for (entry in findNavController().backStack.descendingIterator()) {
            if (entry.destination.id == R.id.nav_graph_user) {
                userDataExists = true
                break
            }
        }

        prefClick(sett.USER, userDataExists) {
            findNavController().navigate(R.id.nav_settings_user)
            true
        }
    }
}


