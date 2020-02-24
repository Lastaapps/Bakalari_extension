package cz.lastaapps.bakalariextension.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.login.Logout

/**
 * App's settings
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId === R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Fragment containing all the settings
     */
    inner class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)




            findPreference<Preference>(getString(R.string.sett_key_log_out))?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    Logout.logout()
                    finish()
                    true
                }
        }
    }

    companion object {
        private val preferenceListener = Preference.OnPreferenceChangeListener()
        { preference: Preference, any: Any ->
            val stringValue: String = any.toString()

            true
        }

        private fun setPreferenceListener(preference: Preference) {
            preference.onPreferenceChangeListener = preferenceListener;

            preferenceListener.onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.key, "")
            )
        }

    }
}