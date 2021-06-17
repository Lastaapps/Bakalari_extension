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

package cz.lastaapps.bakalari.app.initializers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.Keep
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import cz.lastaapps.bakalari.api.io.ConnMgr
import cz.lastaapps.bakalari.api.repo.user.UserChangeObserver
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.NavGraphUserDirections
import cz.lastaapps.bakalari.app.receivers.UserChangedRefresher
import cz.lastaapps.bakalari.app.ui.navigation.ComplexDeepLinkNavigator
import cz.lastaapps.bakalari.app.ui.start.loading.LoadingFragmentDirections
import cz.lastaapps.bakalari.app.ui.start.profiles.ProfilesFragmentDirections
import cz.lastaapps.bakalari.authentication.LoginModuleConfig
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.App
import cz.lastaapps.bakalari.platform.InitializerTemplate
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.reflect.KClass

@Keep
class LegacyInitializer : InitializerTemplate<Any> {

    @Suppress("PrivatePropertyName")
    private val TAG get() = this::class.simpleName

    override fun create(context: Context): Any {
        logCreate()

        initPlatform(context)

        initLogin(context)

        return Any()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        logDependencies()

        //all the app's initializers
        return listOf<KClass<out Initializer<*>>>(
            WorkManagerInitializer::class,
        ).map { it.java }
    }
}

private fun initPlatform(context: Context) {

    App.onCreateTasks += { app, scope ->
        //sets selected theme from Settings
        MySettings.withAppContext().apply {
            updateDarkTheme()
        }

        //deleted user data when new user object is loaded
        app.registerReceiver(UserChangedRefresher(), IntentFilter(UserChangeObserver.USER_CHANGED))
    }

    App.afterCreateTasks += { context, scope ->
        withContext(Dispatchers.Default) {
            val appContext = context.applicationContext
            AccountsDatabase.getDatabase(appContext).repository.refreshSystemAccounts(appContext)
        }
    }
}

private fun initLogin(context: Context) = LoginModuleConfig.apply {

    mainActivity = MainActivity::class

    onInvalidRefreshToken =
        { context -> context.sendBroadcast(Intent(ConnMgr.INVALID_REFRESH_TOKEN)) }

    addAccountIntent = {
        val profilesAction = LoadingFragmentDirections.actionLoadingToProfiles()
        val loginAction = ProfilesFragmentDirections.actionLogin(null)

        ComplexDeepLinkNavigator.createIntent(
            context, mainActivity.java,
            listOf(profilesAction, loginAction)
        )
    }

    editPropertiesIntent = {
        val userAction = LoadingFragmentDirections.actionLoadingToUser(UUID.randomUUID())
        val settingsAction = NavGraphUserDirections.actionGraphSettings()

        ComplexDeepLinkNavigator.createIntent(
            context, mainActivity.java,
            listOf(userAction, settingsAction)
        )
    }
}

