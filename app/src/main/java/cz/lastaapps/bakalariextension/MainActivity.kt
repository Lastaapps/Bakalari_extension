package cz.lastaapps.bakalariextension

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import cz.lastaapps.bakalariextension.api.Login
import cz.lastaapps.bakalariextension.login.Logout
import cz.lastaapps.bakalariextension.send.ReportIssueActivity
import cz.lastaapps.bakalariextension.send.SendIdeaActivity
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        //intent extra to select default fragment
        const val NAVIGATE = "navigate"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_main)

        //toolbar setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //side nav header init
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_logout,
                R.id.nav_share, R.id.nav_rate, R.id.nav_idea,
                R.id.nav_report, R.id.nav_about, R.id.nav_facebook,
                R.id.nav_google_play, R.id.nav_github, R.id.nav_api
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        navView.getHeaderView(0).findViewById<TextView>(R.id.nav_name).text = Login.get(Login.NAME)
        navView.getHeaderView(0).findViewById<TextView>(R.id.nav_type).text = Login.getClassAndRole()

        //init fragment
        val navigateTo = intent.getIntExtra(NAVIGATE, -1)
        if (navigateTo != -1)
            findNavController(R.id.nav_host_fragment).navigate(navigateTo)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_home)
            }
            R.id.nav_timetable -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_timetable)
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
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message) + " https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
            R.id.nav_rate -> {
                val url = "https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension"
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
                val url = "https://play.google.com/store/apps/dev?id=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
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
        }

        val drawer =
            findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}
