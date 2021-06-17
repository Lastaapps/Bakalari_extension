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

package cz.lastaapps.bakalari.app

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import cz.lastaapps.bakalari.api.entity.user.User
import cz.lastaapps.bakalari.api.io.ConnMgr
import cz.lastaapps.bakalari.api.repo.user.UserChangeObserver
import cz.lastaapps.bakalari.app.ui.navigation.ComplexDeepLinkNavigator
import cz.lastaapps.bakalari.app.ui.navigation.navview.ItemsController
import cz.lastaapps.bakalari.app.ui.navigation.navview.NavHeaderController
import cz.lastaapps.bakalari.app.ui.others.TeacherWarning
import cz.lastaapps.bakalari.app.ui.others.WhatsNew
import cz.lastaapps.bakalari.app.ui.start.loading.LoadingFragmentDirections
import cz.lastaapps.bakalari.app.ui.uitools.backStackDebugPrinting
import cz.lastaapps.bakalari.app.ui.user.AppStartInit
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.app.ui.user.CurrentUserHandler
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.BaseActivity
import cz.lastaapps.common.Communication
import cz.lastaapps.common.PlayStoreReview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

/**Activity containing all the content fragments and navigation*/
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private val TAG get() = MainActivity::class.java.simpleName

        //intent extra to select default fragment
        const val NAVIGATE = "navigate"

        const val FULL_STORAGE = "cz.lastaapps.bakalari.app.FULL_STORAGE"
        const val ATTACHMENT_DOWNLOADED = "cz.lastaapps.bakalari.app.ATTACHMENT_DOWNLOADED"
        const val ATTACHMENT_DOWNLOADED_FILENAME =
            "cz.lastaapps.bakalari.app.ATTACHMENT_DOWNLOADED.FileName"
        const val ATTACHMENT_DOWNLOADED_INTENT =
            "cz.lastaapps.bakalari.app.ATTACHMENT_DOWNLOADED.Intent"
        const val ATTACHMENT_DOWNLOADED_URI =
            "cz.lastaapps.bakalari.app.ATTACHMENT_DOWNLOADED.Uri"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val mainViewModel: MainViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var navHeaderController: NavHeaderController
    private lateinit var itemsController: ItemsController

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {

        //used to remove splashscreen
        setTheme(cz.lastaapps.bakalari.core.R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating MainActivity")

        setContentView(R.layout.activity_main)

        //goes fullscreen in landscape mode
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }

        //toolbar setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //side nav header init
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        //finds controller used for switching fragments
        val navController = findNavController()

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        //this ones does not show back arrow when navigated to, but the tree lines of selection
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_loading,
                R.id.nav_profiles,
                cz.lastaapps.bakalari.authentication.R.id.nav_login,
                R.id.nav_home,
                R.id.nav_timetable,
                R.id.nav_marks,
                R.id.nav_homework,
                R.id.nav_teacher_list,
                R.id.nav_subject_list,
                R.id.nav_absence,
                R.id.nav_events
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        //navigation back stack printing for debugging
        navController.backStackDebugPrinting(TAG)

        CurrentUserHandler(this)
        onIntentChanged(intent)

        //receivers
        //shows popup when refresh token is invalid
        registerReceiver(invalidRefreshTokenReceiver, IntentFilter(ConnMgr.INVALID_REFRESH_TOKEN))
        //shows popup when storage is full
        registerReceiver(fullStorageReceiver, IntentFilter(FULL_STORAGE))
        //shows popup when user object changes and the data should be reloaded
        registerReceiver(userChangedReceiver, IntentFilter(UserChangeObserver.USER_CHANGED))
        //shows popup when attachment downloaded with the opinion to open it
        registerReceiver(attachmentDownloaded, IntentFilter(ATTACHMENT_DOWNLOADED))

        navHeaderController = NavHeaderController(this)
        itemsController = ItemsController(this)

        lifecycleScope.launch(Dispatchers.Main) {
            CurrentUser.accountUUID.consumeEach { uuid ->
                if (uuid != null) {
                    userViewModel.runOrRefresh(lifecycle, lifecycleScope) {
                        setupWithUserData(it)
                    }
                }
            }
        }

        fakeNotification()
    }

    //TODO remove
    private fun fakeNotification() {
        val context = this

        return //disabled

        lifecycleScope.launch {
            AccountsDatabase.getDatabase(context).repository.getAllObservable().collect {
                if (it.isNotEmpty()) {

                    val userAction = LoadingFragmentDirections.actionLoadingToUser(it[0].uuid)
                    val settingsAction = NavGraphUserDirections.actionGraphSettings()

                    val intent = ComplexDeepLinkNavigator.createIntent(
                        context, MainActivity::class.java,
                        listOf(userAction, settingsAction)
                    )

                    val pending = PendingIntent.getActivity(
                        context,
                        24561,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val notification =
                        NotificationCompat.Builder(
                            context,
                            getString(cz.lastaapps.bakalari.platform.R.string.channel_general_id)
                        )
                            .setContentText("Pokus")
                            .setContentIntent(pending)
                            .setSmallIcon(cz.lastaapps.bakalari.core.R.drawable.icon)
                            .setAutoCancel(false)
                            .build()

                    NotificationManagerCompat.from(context).notify(notifiId, notification)
                }
            }
        }
    }

    val notifiId = 45612

    override fun onDestroy() {
        super.onDestroy()

        NotificationManagerCompat.from(this).cancel(notifiId)

        unregisterReceiver(invalidRefreshTokenReceiver)
        unregisterReceiver(fullStorageReceiver)
        unregisterReceiver(userChangedReceiver)
        unregisterReceiver(attachmentDownloaded)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "New intent")

        onIntentChanged(intent)
    }

    private fun onIntentChanged(intent: Intent) {

        findNavController().handleDeepLink(intent)
        /*intent.dataString?.let {
            findNavController().navigate(it.toUri())
        }*/

        ComplexDeepLinkNavigator(findNavController()).handle(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController()
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**options in the top right corner*/
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (onNavigationItemSelected(item)) true else super.onOptionsItemSelected(item)

    override fun onBackPressed() {
        if (hideNav()) return

        super.onBackPressed()
    }

    /**@return if drawer layout closed*/
    @MainThread
    fun hideNav(): Boolean {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        return if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        } else false
    }

    @MainThread
    fun doAccountChange(requested: UUID?) {
        findNavController().navigate(NavGraphRootDirections.actionLoading(requested, false))

        hideNav()
    }

    //TODO loginCheck
    fun loginCheckDone() {
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

        findNavController().apply {

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
    private fun setupWithUserData(user: User) {

        //TODO move elsewhere
        //What's new - shown only once per version
        if (WhatsNew(this).shouldShow()) {
            WhatsNew(this).showDialog()
        }

        if (TeacherWarning.shouldShow(this, user))
            TeacherWarning().show(supportFragmentManager)
    }

    /**When side navigation or the bottom bar was selected*/
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "Navigation item selected")

        if (itemsController.onNavigationItemSelected(item)) return true

        when (item.itemId) {
            R.id.nav_settings -> {
                findNavController().navigate(R.id.nav_graph_settings)
            }
            R.id.nav_share -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        getString(R.string.share_message) + " https://play.google.com/store/apps/details?id=cz.lastaapps.bakalari.app"
                    )
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
            R.id.nav_rate -> {
                PlayStoreReview.doInAppReview(this)
            }
            R.id.nav_idea -> {
                val args = bundleOf("uuid" to CurrentUser.accountUUID.value)
                findNavController().navigate(cz.lastaapps.bakalari.report.R.id.idea_navigation, args)
            }
            R.id.nav_report -> {
                val args = bundleOf("uuid" to CurrentUser.accountUUID.value)
                findNavController().navigate(cz.lastaapps.bakalari.report.R.id.report_navigation, args)
            }
            R.id.nav_about -> {
                findNavController().navigate(R.id.nav_about)
            }
            R.id.nav_facebook -> {
                Communication.openFacebook(this)
            }
            R.id.nav_google_play -> {
                Communication.openPlayStore(this)
            }
            R.id.nav_github -> {
                Communication.openProjectsGithub(this, "bakalari_extension")
            }
            R.id.nav_api -> {
                val url = "https://github.com/bakalari-api"
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.nav_license -> {
                findNavController().navigate(R.id.nav_license)
            }

            R.id.nav_profiles_list -> {
                doAccountChange(null)
            }

            else -> return false
        }

        hideNav()

        return true
    }

    private val invalidRefreshTokenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ConnMgr.INVALID_REFRESH_TOKEN) {
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
            if (intent.action == UserChangeObserver.USER_CHANGED) {
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

                findNavController().navigate(
                    NavGraphRootDirections.actionAttachmentDownloaded(
                        intent.getStringExtra(ATTACHMENT_DOWNLOADED_FILENAME)!!,
                        intent.getParcelableExtra(ATTACHMENT_DOWNLOADED_INTENT)!!,
                        intent.getParcelableExtra(ATTACHMENT_DOWNLOADED_URI)!!
                    )
                )
            }
        }
    }

    fun findNavController(): NavController = findNavController(R.id.nav_host_fragment)
}
