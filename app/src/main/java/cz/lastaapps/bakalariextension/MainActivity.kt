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

package cz.lastaapps.bakalariextension

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.login.Logout
import cz.lastaapps.bakalariextension.send.ReportIssueActivity
import cz.lastaapps.bakalariextension.send.SendIdeaActivity
import cz.lastaapps.bakalariextension.tools.BaseActivity
import cz.lastaapps.bakalariextension.ui.WhatsNew
import cz.lastaapps.bakalariextension.ui.license.LicenseActivity
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**Activity containing all the content fragments and navigation*/
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        //intent extra to select default fragment
        const val NAVIGATE = "navigate"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating MainActivity")

        //goes fullscreen in landscape mode
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        setContentView(R.layout.activity_main)

        //toolbar setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //side nav header init
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        //finds controller used for switching fragments
        val navController = findNavController(R.id.nav_host_fragment)

        //sets up bottom navigation with same navController
        findViewById<BottomNavigationView>(R.id.bottom_nav)
            .setupWithNavController(navController)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        //this ones does not show back arrow when navigated to, but the tree lines of selection
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_timetable, R.id.nav_marks,
                R.id.nav_homework, R.id.nav_teacher_list, R.id.nav_subject_list
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        //updates side nav with info
        navView.getHeaderView(0).findViewById<TextView>(R.id.nav_name).text = User.get(User.NAME)
        navView.getHeaderView(0).findViewById<TextView>(R.id.nav_type).text = User.getClassAndRole()

        //selects default fragment
        val navigateTo = intent.getIntExtra(NAVIGATE, -1)
        if (navigateTo != -1)
            findNavController(R.id.nav_host_fragment).navigate(navigateTo)

        //What's new - shown only once per version
        if (WhatsNew(this).shouldShow()) {
            WhatsNew(this).showDialog()
        }

        //only on the first onCreate() call
        if (savedInstanceState == null)
            launchInit()
    }

    /**Inits services, alarms and widgets*/
    private fun launchInit() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            AppStartInit(applicationContext).appStartInit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**options in the top right corner*/
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (onNavigationItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    /**When side navigation or the bottom bar was selected*/
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "Navigation item selected")

        var toReturn = true

        when (item.itemId) {
            R.id.nav_home -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_home)
            }
            R.id.nav_timetable -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_timetable)
            }
            R.id.nav_marks -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_marks)
            }
            R.id.nav_homework -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_homework)
            }
            R.id.nav_teacher_list -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_teacher_list)
            }
            R.id.nav_subject_list -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_subject_list)
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_logout -> {
                Logout.logout()
                startActivity(Intent(this, LoadingActivity::class.java))
                finish()
            }
            R.id.nav_share -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        getString(R.string.share_message) + " https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension"
                    )
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
            R.id.nav_rate -> {
                val url =
                    "https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.nav_idea -> {
                val intent = Intent(this, SendIdeaActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_report -> {
                val intent = Intent(this, ReportIssueActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_about -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_about)
            }
            R.id.nav_facebook -> {
                val url = "https://www.facebook.com/lastaapps/"
                var uri = Uri.parse(url)
                try {
                    val applicationInfo =
                        packageManager.getApplicationInfo("com.facebook.katana", 0)
                    if (applicationInfo.enabled) {
                        uri = Uri.parse("fb://facewebmodal/f?href=$url")
                    }
                } catch (ignored: PackageManager.NameNotFoundException) {
                }
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.nav_google_play -> {
                //TODO app play store link
                val url =
                    "https://play.google.com/store/apps/dev?id=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.nav_github -> {
                val url = "https://github.com/lastaapps/bakalari_extension"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.nav_api -> {
                val url = "https://github.com/bakalari-api"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.nav_license -> {
                startActivity(Intent(this, LicenseActivity::class.java))
            }
            else -> {
                toReturn = false
            }
        }

        val drawer =
            findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)

        return toReturn
    }
}
