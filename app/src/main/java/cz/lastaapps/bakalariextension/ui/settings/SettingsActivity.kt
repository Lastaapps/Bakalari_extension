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

package cz.lastaapps.bakalariextension.ui.settings

import android.app.backup.BackupManager
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.login.Logout
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.tools.BaseActivity
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.ui.WhatsNew
import cz.lastaapps.bakalariextension.ui.license.LicenseActivity
import cz.lastaapps.bakalariextension.ui.loading.LoadingFragment


/**
 * Manages settings UI, actual settings are saved in *.tools.Settings
 */
class SettingsActivity : BaseActivity() {

    companion object {
        private val TAG = SettingsActivity::class.java.simpleName

        //savedInstanceBundle entry telling, that after settings are closed,
        // whole app needs to be reloaded
        private const val RELAUNCH_APP = "RELAUNCH"
    }

    //if any critical setting was changed and app needs to reload completely
    private var relaunchApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating settings activity")

        relaunchApp = intent.extras?.getBoolean(RELAUNCH_APP, false) ?: false

        setContentView(R.layout.activity_settings)
        //puts in Fragment containing settings
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.settings,
                SettingsFragment()
            )
            .commit()

        //puts back arrow into the top left corner
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {

        //notifies that backup should be made
        BackupManager.dataChanged(App.context.packageName)

        if (relaunchApp) {
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
        } else
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing)
        //notifies that backup should be made
            BackupManager.dataChanged(App.context.packageName)
    }

    /**Fragment with all the settings*/
    class SettingsFragment : PreferenceFragmentCompat() {

        lateinit var sett: MySettings

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

            Log.i(TAG, "Creating preferences")

            //reference to actual Settings
            sett = MySettings(requireContext())

            sett.initSettings()
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            //sets actions for all the actions

            //notification settings
            fp(sett.NOTIFICATION_SETTINGS)?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
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
            fp(sett.MOBILE_DATA)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Mobile data changed to $newValue")
                true
            }

            //dark mode
            fp(sett.DARK_MODE)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Dark theme changed, selected $newValue")

                sett.updateDarkTheme(newValue.toString())
                true
            }

            //language
            fp(sett.LANGUAGE)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Language changed to $newValue")

                //relaunches app
                Handler(Looper.getMainLooper()).postDelayed({

                    val intent = Intent(activity, SettingsActivity::class.java)
                    intent.putExtra(RELAUNCH_APP, true)
                    activity?.startActivity(intent)
                    activity?.finish()

                }, 10)

                true
            }

            //logout
            fp(sett.LOGOUT)?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    Log.i(TAG, "Login out")
                    Logout.logout()
                    activity?.finish()
                    startActivity(Intent(this.activity, LoadingFragment::class.java))
                    true
                }

            //download location
            fp(sett.DOWNLOAD_LOCATION)?.apply {
                onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Log.i(TAG, "Download location changing")

                        sett.chooseDownloadDirectory(requireActivity()) {
                            setDownloadLocationText(this)
                        }

                        true
                    }
                setDownloadLocationText(this)
            }

            //from which day should be next week shown
            fp(sett.TIMETABLE_DAY)?.setOnPreferenceChangeListener { _, newValue ->
                Log.i(TAG, "Show new timetable day changed to $newValue")
                true
            }

            //if notification informing about timetable should be shown
            fp(sett.TIMETABLE_NOTIFICATION)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Timetable notification changed to $newValue")
                Handler(Looper.getMainLooper()).postDelayed({
                    TTNotifyService.startService(preference.context)
                }, 100)
                true
            }

            //from which day should be next week shown
            fp(sett.TIMETABLE_PREVIEW)?.setOnPreferenceChangeListener { _, newValue ->
                Log.i(TAG, "Show preview for tomorrow day changed to $newValue")
                true
            }

            //if user's town and school can be send in analytics and reports
            fp(sett.SEND_TOWN_SCHOOL)?.setOnPreferenceChangeListener { _, newValue ->
                Log.i(TAG, "Send town and school changed to $newValue")
                true
            }

            //resets settings
            fp(sett.RESET)?.setOnPreferenceClickListener { _ ->
                Log.i(TAG, "!!! Resetting settings !!!")

                sett.getSP().edit().clear().apply()
                sett.initSettings()

                preferenceScreen = null
                addPreferencesFromResource(R.xml.settings_preferences)

                //relaunches app
                Handler(Looper.getMainLooper()).postDelayed({

                    val intent = Intent(activity, SettingsActivity::class.java)
                    intent.putExtra(RELAUNCH_APP, true)
                    activity?.startActivity(intent)
                    activity?.finish()

                }, 10)

                true
            }

            //shows whats new
            fp(sett.SHOW_WHATS_NEW)?.setOnPreferenceClickListener { _ ->
                Log.i(TAG, "Showing What's new")

                WhatsNew(requireContext()).showDialog()

                true
            }

            //shows license
            fp(sett.LICENSE)?.setOnPreferenceClickListener { _ ->
                Log.i(TAG, "Showing license")

                LicenseActivity.viewLicense(requireContext())

                true
            }

        }

        override fun onResume() {
            super.onResume()

            //if location was changed by oder activity
            fp(sett.DOWNLOAD_LOCATION)?.let {
                setDownloadLocationText(it)
            }
        }

        /**
         * Represents findPreference<Preference>
         */
        private fun fp(key: String): Preference? = findPreference(key)

        /**Sets readable text instead of content Uri on Q+
         * replaces*/
        private fun setDownloadLocationText(preference: Preference) {

            var result: String? = ""
            val stringUri = Uri.decode(sett.getDownloadLocation())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

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

            } else {
                result = stringUri
            }
            preference.summary = result ?: ""
        }

    }
}