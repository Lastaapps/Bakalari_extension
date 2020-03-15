package cz.lastaapps.bakalariextension.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import cz.lastaapps.bakalariextension.LoadingActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.login.Logout
import cz.lastaapps.bakalariextension.tools.App
import java.util.*


/**
 * App's settings
 */
class SettingsActivity : AppCompatActivity() {

    private var relaunchApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        relaunchApp = intent.extras?.getBoolean(RELAUNCH_APP, false) ?: false

        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (relaunchApp) {
            startActivity(Intent(this, LoadingActivity::class.java))
            finish()
        } else
            super.onBackPressed()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            initSettings()
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            fp(MOBILE_DATA)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Mobile data changed to $newValue")
                true
            }

            //dark mode
            fp(DARK_MODE)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Dark theme changed, selected $newValue")

                updateDarkTheme(newValue.toString())
                true
            }

            //language
            fp(LANGUAGE)?.setOnPreferenceChangeListener { preference, newValue ->
                Log.i(TAG, "Language changed to $newValue")

                updateLanguage(context!!, newValue.toString())

                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(activity, SettingsActivity::class.java)
                    intent.putExtra(RELAUNCH_APP, true)
                    activity?.startActivity(intent)
                    activity?.finish()
                }, 100)

                true
            }

            //logout
            fp(LOGOUT)?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    Log.i(TAG, "Login out")
                    Logout.logout()
                    activity?.finish()
                    startActivity(Intent(this.activity, LoadingActivity::class.java))
                    true
                }
        }

        /**
         * Represents findPreference<Preference>
         */
        private inline fun fp(key: String): Preference? {
            return findPreference(key)
        }
    }

    companion object {
        private val TAG = SettingsActivity::class.java.simpleName

        private const val RELAUNCH_APP = "RELAUNCH"

        val MOBILE_DATA = App.getString(R.string.sett_key_mobile_data)
        val DARK_MODE = App.getString(R.string.sett_key_dark_mode)
        val LANGUAGE = App.getString(R.string.sett_key_language)
        val LOGOUT = getString(R.string.sett_key_log_out)

        fun initSettings() {
            val sp = getSP()
            val editor = sp.edit()

            Log.d(TAG, sp.getBoolean(MOBILE_DATA, false).toString())
            Log.d(TAG, sp.getString(LANGUAGE, ""))
            Log.d(TAG, sp.getString(DARK_MODE, ""))

            if (sp.getString(LANGUAGE, "") == "")
                editor.putString(LANGUAGE, getArray(R.array.sett_language)[0])

            if (sp.getString(DARK_MODE, "") == "")
                editor.putString(DARK_MODE, getArray(R.array.sett_dark_mode)[2])

            editor.apply()
        }

        fun updateLanguage(
            context: Context,
            value: String = getSP().getString(LANGUAGE, "").toString()
        ) {

            val array: Array<String> = getArray(R.array.sett_language)
            val index = array.indexOf(value)
            val languageCode = getLanguageCode(index)
            val res: Resources = context.resources
            // Change locale settings in the app.
            val dm: DisplayMetrics = res.displayMetrics
            val conf: Configuration = res.configuration
            conf.setLocale(Locale(languageCode.toLowerCase())) // API 17+ only.

            res.updateConfiguration(conf, dm)
        }

        private fun getLanguageCode(i: Int): String {
            val array: Array<String> = arrayOf(Locale.getDefault().language, "cs", "en")
            return array[i.coerceAtLeast(0)]
        }

        fun updateDarkTheme(value: String = getSP().getString(DARK_MODE, "").toString()): Boolean {
            val array: Array<String> = getArray(R.array.sett_dark_mode)

            val toChange = when (array.indexOf(value)) {
                0 -> AppCompatDelegate.MODE_NIGHT_NO
                1 -> AppCompatDelegate.MODE_NIGHT_YES
                else ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            }

            if (toChange == AppCompatDelegate.getDefaultNightMode())
                return false

            AppCompatDelegate.setDefaultNightMode(toChange)
            return true
        }

        inline fun getSP(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(App.context)
        }

        private inline fun getString(id: Int): String {
            return App.context.getString(id)
        }

        private inline fun getArray(id: Int): Array<String> {
            return App.context.resources.getStringArray(id)
        }
    }
}