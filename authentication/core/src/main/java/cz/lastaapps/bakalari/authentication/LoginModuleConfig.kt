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

package cz.lastaapps.bakalari.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlin.reflect.KClass

object LoginModuleConfig {

    //TODO implement in initializer
    lateinit var mainActivity: KClass<out Activity>

    lateinit var onInvalidRefreshToken: ((Context) -> Unit)
    /*
        context.sendBroadcast(Intent(MainActivity.INVALID_REFRESH_TOKEN))
    */

    lateinit var addAccountIntent: ((Context) -> Intent)
    /*
        val profilesAction = LoadingFragmentDirections.actionLoadingToProfiles()
        val loginAction = ProfilesFragmentDirections.actionLogin(null)

        val intent = ComplexDeepLinkNavigator.createIntent(
            context, LoginModuleConfig.mainActivity.java,
            listOf(profilesAction, loginAction)
        )
    */

    lateinit var editPropertiesIntent: ((Context) -> Intent)
    /*
        val userAction = LoadingFragmentDirections.actionLoadingToUser(UUID.randomUUID())
        val settingsAction = NavGraphUserDirections.actionGraphSettings()

        val intent = ComplexDeepLinkNavigator.createIntent(
            context, LoginModuleConfig.mainActivity!!.java,
            listOf(userAction, settingsAction)
        )
    */

}