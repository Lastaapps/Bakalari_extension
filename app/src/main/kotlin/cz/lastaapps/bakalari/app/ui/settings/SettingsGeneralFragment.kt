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

import android.app.backup.BackupManager
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.uitools.settingsViewModels
import cz.lastaapps.bakalari.platform.App
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.DownloadLocationContract
import cz.lastaapps.bakalari.settings.MySettings


/**
 * Manages settings UI, actual settings are saved in *.tools.Settings
 * Fragment with all the settings
 */
class SettingsGeneralFragment : PreferenceFragmentCompat() {

    companion object {
        private val TAG = SettingsGeneralFragment::class.java.simpleName
    }


    private val viewModel: SettingsViewModel by settingsViewModels()
    private lateinit var sett: MySettings

    override fun onDestroy() {
        super.onDestroy()

        if (viewModel.relaunchApp) {
            if (!requireActivity().isChangingConfigurations) {
                //notifies that backup should be made
                BackupManager.dataChanged(App.context.packageName)

                restartProcess()
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        Log.i(TAG, "Creating preferences")

        //reference to actual Settings
        sett = MySettings.withAppContext()

        sett.initSettings()
        setPreferencesFromResource(R.xml.settings_general, rootKey)

        //sets actions for all the actions


        //GENERAL
        //notification settings
        prefClick(sett.NOTIFICATION_SETTINGS, true) { _ ->
            Log.i(TAG, "Opening notification settings")

            val intent = Intent()
            val context = requireContext()

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    intent.putExtra("app_package", context.packageName)
                    intent.putExtra("app_uid", context.applicationInfo.uid)
                }
                else -> {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.data = Uri.parse("package:" + context.packageName)
                }
            }

            startActivity(intent)

            true
        }

        //if app can use mobile data in background
        prefChange(sett.MOBILE_DATA, true) { preference, newValue ->
            Log.i(TAG, "Mobile data changed to $newValue")
            true
        }

        //dark mode
        prefChange(sett.DARK_MODE, true) { preference, newValue ->
            Log.i(TAG, "Dark theme changed, selected $newValue")

            sett.updateDarkTheme(newValue.toString())
            true
        }

        //language
        prefChange(sett.LANGUAGE, true) { preference, newValue ->
            Log.i(TAG, "Language changed to $newValue")

            //relaunches app
            Handler(Looper.getMainLooper()).postDelayed({

                viewModel.relaunchApp = true

            }, 10)

            true
        }

        //download location
        prefClick(sett.DOWNLOAD_LOCATION, true) {

            Log.i(TAG, "Download location changing")


            true
        }
        fp(sett.DOWNLOAD_LOCATION)?.let { setDownloadLocationText(it) }

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
