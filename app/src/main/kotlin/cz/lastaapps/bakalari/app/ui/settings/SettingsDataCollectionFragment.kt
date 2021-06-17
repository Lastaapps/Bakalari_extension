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

import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.uitools.settingsViewModels
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings

class SettingsDataCollectionFragment : PreferenceFragmentCompat() {

    companion object {
        private val TAG get() = SettingsDataCollectionFragment::class.simpleName
    }

    private val viewModel: SettingsViewModel by settingsViewModels()
    private lateinit var sett: MySettings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        Log.i(TAG, "Creating preferences")

        //reference to actual Settings
        sett = MySettings.withAppContext()

        sett.initSettings()
        setPreferencesFromResource(R.xml.settings_data_collection, rootKey)


        //ANALYTICS
        //if user's town and school can be send in analytics and reports
        prefChange(sett.SEND_TOWN_SCHOOL, true) { _, newValue ->
            Log.i(TAG, "Send town and school changed to $newValue")
            true
        }
    }
}