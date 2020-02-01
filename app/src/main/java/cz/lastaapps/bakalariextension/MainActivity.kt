package cz.lastaapps.bakalariextension

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.google.firebase.analytics.FirebaseAnalytics
import cz.lastaapps.bakalariextension.apimodules.Login
import cz.lastaapps.bakalariextension.send.ReportIssueActivity
import cz.lastaapps.bakalariextension.send.SendIdeaActivity


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        */

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_about,
                R.id.nav_share, R.id.nav_rate, R.id.nav_idea,
                R.id.nav_report, R.id.nav_facebook, R.id.nav_google_play,
                R.id.nav_github, R.id.nav_api
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)


    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()

        val nameView = findViewById<TextView>(R.id.name)
        val typeView = findViewById<TextView>(R.id.type)
        val schoolView = findViewById<TextView>(R.id.school)

        nameView.text = Login.get(Login.NAME)
        typeView.text = "${Login.get(Login.CLASS)} - ${Login.parseRole(Login.get(Login.ROLE))}"
        schoolView.text = Login.get(Login.SCHOOL)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.nav_home -> {
            }
            R.id.nav_settings -> {
            }
            R.id.nav_about -> {
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
                //TODO
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
