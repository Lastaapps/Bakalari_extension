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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.review.ReviewManagerFactory
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.send.ReportIssueActivity
import cz.lastaapps.bakalariextension.send.SendIdeaActivity
import cz.lastaapps.bakalariextension.tools.BaseActivity
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.bottom.BottomFragment
import cz.lastaapps.bakalariextension.ui.bottom.BottomItem
import cz.lastaapps.bakalariextension.ui.login.ActionsLogout
import cz.lastaapps.bakalariextension.ui.others.TeacherWarning
import cz.lastaapps.bakalariextension.ui.others.WhatsNew
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**Activity containing all the content fragments and navigation*/
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        //intent extra to select default fragment
        const val NAVIGATE = "navigate"

        const val INVALID_REFRESH_TOKEN = "cz.lastaapps.bakalariextension.INVALID_REFRESH_TOKEN"
        const val FULL_STORAGE = "cz.lastaapps.bakalariextension.FULL_STORAGE"
        const val USER_CHANGED = "cz.lastaapps.bakalariextension.USER_CHANGED"
        const val ATTACHMENT_DOWNLOADED = "cz.lastaapps.bakalariextension.ATTACHMENT_DOWNLOADED"
        const val ATTACHMENT_DOWNLOADED_FILENAME =
            "cz.lastaapps.bakalariextension.ATTACHMENT_DOWNLOADED.FileName"
        const val ATTACHMENT_DOWNLOADED_INTENT =
            "cz.lastaapps.bakalariextension.ATTACHMENT_DOWNLOADED.Intent"
        const val ATTACHMENT_DOWNLOADED_URI =
            "cz.lastaapps.bakalariextension.ATTACHMENT_DOWNLOADED.Uri"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val mainViewModel: MainViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating MainActivity")

        //goes fullscreen in landscape mode
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
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

        //manages loading and navigation view visibility changes
        if (mainViewModel.result.value == MainViewModel.UNKNOWN) {
            navController.navigate(R.id.nav_loading)
        } else if (mainViewModel.loggedIn.value == true) {
            setupWithUserData()
        }

        //sets up bottom navigation with same navController
        //findViewById<BottomNavigationView>(R.id.bottom_nav)
        //    .setupWithNavController(navController)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        //this ones does not show back arrow when navigated to, but the tree lines of selection
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_timetable, R.id.nav_marks,
                R.id.nav_homework, R.id.nav_teacher_list, R.id.nav_subject_list,
                R.id.nav_absence, R.id.nav_events
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        //shows popup when refresh token is invalid
        registerReceiver(invalidRefreshTokenReceiver, IntentFilter(INVALID_REFRESH_TOKEN))

        //shows popup when storage is full
        registerReceiver(fullStorageReceiver, IntentFilter(FULL_STORAGE))

        //shows popup when user object changes and the data should be reloaded
        registerReceiver(userChangedReceiver, IntentFilter(USER_CHANGED))

        //shows popup when attachment downloaded with the opinion to open it
        registerReceiver(attachmentDownloaded, IntentFilter(ATTACHMENT_DOWNLOADED))
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(invalidRefreshTokenReceiver)
        unregisterReceiver(fullStorageReceiver)
        unregisterReceiver(userChangedReceiver)
        unregisterReceiver(attachmentDownloaded)
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

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        super.onBackPressed()
    }

    fun loginCheckDone() {

        setupWithUserData()

        findNavController(R.id.nav_host_fragment).apply {

            //removes all navigation destinations from stack except default/home one - nav_home
            popBackStack()

            //show some externally required fragment
            val navigateTo = intent.getIntExtra(NAVIGATE, -1)
            if (navigateTo != -1) {
                navigate(navigateTo)
            }
        }
    }

    /**shows home fragment*/
    private fun setupWithUserData() {
        mainViewModel.loggedIn.value = true

        mainViewModel.launchInitRun.apply {
            if (value == false) {
                value = true

                //Inits services, alarms and widgets
                val scope = CoroutineScope(Dispatchers.Default)
                scope.launch {
                    AppStartInit(this@MainActivity).appStartInit()
                }
            }
        }

        findViewById<View>(R.id.bottom_nav).visibility = View.VISIBLE
        findViewById<View>(R.id.nav_view).visibility = View.VISIBLE
        findViewById<View>(R.id.appBarLayout).visibility = View.VISIBLE

        //What's new - shown only once per version
        if (WhatsNew(this).shouldShow()) {
            WhatsNew(this).showDialog()
        }

        if (TeacherWarning.shouldShow(this, userViewModel.requireData()))
            TeacherWarning().show(supportFragmentManager)

        setupNavMenus()
    }

    /**adds navigation items of the enabled modules*/
    private fun setupNavMenus() {

        //updates side nav with info
        val user = userViewModel.requireData()
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.getHeaderView(0).findViewById<TextView>(R.id.nav_name).text = user.normalFunName
        navView.getHeaderView(0).findViewById<TextView>(R.id.nav_type).text = user.getClassAndRole()

        var firstOrder = 0

        navView.menu.add(
            R.id.nav_items_group,
            R.id.nav_home,
            firstOrder,
            R.string.menu_home
        ).setIcon(R.drawable.nav_home)

        if (user.isModuleEnabled(User.TIMETABLE))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_timetable,
                ++firstOrder,
                R.string.menu_timetable
            ).setIcon(R.drawable.nav_timetable)

        if (user.isModuleEnabled(User.MARKS))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_marks,
                ++firstOrder,
                R.string.menu_marks
            ).setIcon(R.drawable.nav_marks)

        if (user.isModuleEnabled(User.HOMEWORK))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_homework,
                ++firstOrder,
                R.string.menu_homework
            ).setIcon(R.drawable.nav_homework)

        if (user.isModuleEnabled(User.EVENTS))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_events,
                ++firstOrder,
                R.string.menu_events
            ).setIcon(R.drawable.nav_events)

        if (user.isModuleEnabled(User.ABSENCE))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_absence,
                ++firstOrder,
                R.string.menu_absence
            ).setIcon(R.drawable.nav_absence)

        if (user.isModuleEnabled(User.SUBJECTS))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_teacher_list,
                ++firstOrder,
                R.string.menu_teacher_list
            ).setIcon(R.drawable.nav_teacher)

        if (user.isModuleEnabled(User.SUBJECTS))
            navView.menu.add(
                R.id.nav_items_group,
                R.id.nav_subject_list,
                ++firstOrder,
                R.string.menu_subject_list
            ).setIcon(R.drawable.nav_subjects)


        //BOTTOM navigation
        val bottom =
            //findViewById<BottomNavigationView>(R.id.bottom_nav)
            (supportFragmentManager.findFragmentByTag(getString(R.string.bottom_fragment_tag)) as BottomFragment)

        bottom.items.add(
            BottomItem(
                R.id.nav_home,
                R.string.menu_home,
                R.drawable.module_home
            )
        )

        if (user.isModuleEnabled(User.TIMETABLE))
            bottom.items.add(
                BottomItem(
                    R.id.nav_timetable,
                    R.string.menu_timetable,
                    R.drawable.module_timetable
                )
            )

        if (user.isModuleEnabled(User.MARKS))
            bottom.items.add(
                BottomItem(
                    R.id.nav_marks,
                    R.string.menu_marks,
                    R.drawable.module_marks
                )
            )

        if (user.isModuleEnabled(User.HOMEWORK))
            bottom.items.add(
                BottomItem(
                    R.id.nav_homework,
                    R.string.menu_homework,
                    R.drawable.module_homework
                )
            )

        if (user.isModuleEnabled(User.EVENTS))
            bottom.items.add(
                BottomItem(
                    R.id.nav_events,
                    R.string.menu_events,
                    R.drawable.module_events
                )
            )

        if (user.isModuleEnabled(User.ABSENCE))
            bottom.items.add(
                BottomItem(
                    R.id.nav_absence,
                    R.string.menu_absence,
                    R.drawable.module_absence
                )
            )

        if (user.isModuleEnabled(User.SUBJECTS))
            bottom.items.add(
                BottomItem(
                    R.id.nav_teacher_list,
                    R.string.menu_teacher_list,
                    R.drawable.module_teacher
                )
            )

        if (user.isModuleEnabled(User.SUBJECTS))
            bottom.items.add(
                BottomItem(
                    R.id.nav_subject_list,
                    R.string.menu_subject_list,
                    R.drawable.module_subjects
                )
            )

        bottom.dataUpdated()
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
            R.id.nav_absence -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_absence)
            }
            R.id.nav_teacher_list -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_teacher_list)
            }
            R.id.nav_subject_list -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_subject_list)
            }
            R.id.nav_settings -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_settings)
            }
            R.id.nav_logout -> {
                CoroutineScope(Dispatchers.Default).launch {
                    ActionsLogout.logout()
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@MainActivity, MainActivity::class.java))
                        finish()
                    }
                }
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

                val manager = ReviewManagerFactory.create(this@MainActivity)

                //redirects to the play store, required under LOLLIPOP 5.0 and when Google play
                //API fails
                val oldRequest = {
                    val url =
                        "https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension"
                    val uri = Uri.parse(url)
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }

                //version check
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    oldRequest()
                    return true
                }

                //Google play in app review
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { request ->
                    if (request.isSuccessful) {

                        val reviewInfo = request.result
                        val flow = manager.launchReviewFlow(this@MainActivity, reviewInfo)
                        flow.addOnCompleteListener { _ ->
                            Toast.makeText(
                                this@MainActivity,
                                R.string.thanks_review,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        oldRequest()
                    }
                }
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
                findNavController(R.id.nav_host_fragment).navigate(R.id.nav_license)
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

    private val invalidRefreshTokenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == INVALID_REFRESH_TOKEN) {
                Log.i(TAG, "Showing invalid token message")

                //shows message only once
                if (mainViewModel.invalidTokenMessageShown.value == false) {

                    mainViewModel.invalidTokenMessageShown.value = true

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.invalid_token_understand_title)
                        .setMessage(R.string.invalid_token_understand_message)
                        .setPositiveButton(R.string.invalid_token_understand_button) { _, _ -> }
                        .setCancelable(true)
                        .show()
                }
            }
        }
    }

    private val fullStorageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FULL_STORAGE) {
                Log.i(TAG, "Showing full storage message")

                //shows message only once
                if (mainViewModel.fullStorageShown.value == false) {

                    mainViewModel.fullStorageShown.value = true

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.full_storage_title)
                        .setMessage(R.string.full_storage_message)
                        .setPositiveButton(R.string.full_storage_button) { _, _ -> }
                        .setCancelable(true)
                        .show()
                }
            }
        }
    }

    private val userChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == USER_CHANGED) {
                Log.i(TAG, "User object changed")

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.user_changed_title)
                    .setMessage(R.string.user_changed_message)
                    .setPositiveButton(R.string.user_changed_button) { _, _ ->
                        startActivity(
                            Intent(this@MainActivity, MainActivity::class.java)
                        )
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private val attachmentDownloaded = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ATTACHMENT_DOWNLOADED) {
                Log.i(TAG, "Attachment downloaded received, navigation to the destination")

                findNavController(R.id.nav_host_fragment).navigate(
                    MobileNavigationDirections.actionAttachmentDownloaded(
                        intent.getStringExtra(ATTACHMENT_DOWNLOADED_FILENAME)!!,
                        intent.getParcelableExtra(ATTACHMENT_DOWNLOADED_INTENT)!!,
                        intent.getParcelableExtra(ATTACHMENT_DOWNLOADED_URI)!!
                    )
                )
            }
        }

    }
}
