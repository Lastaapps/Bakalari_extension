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

package cz.lastaapps.bakalari.app.ui.user

import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.app.ui.uitools.observeForControllerGraphChanges
import cz.lastaapps.bakalari.app.ui.user.home.HomeFragmentArgs
import kotlinx.coroutines.*

class CurrentUserHandler(val activity: MainActivity) {

    init {
        val controller = activity.findNavController()

        controller.addOnDestinationChangedListener { _, destination, arguments ->

            if (destination.id == R.id.nav_home) {

                val uuid =
                    if (arguments != null)
                        try {
                            HomeFragmentArgs.fromBundle(arguments).uuid
                        } catch (e: Exception) {
                            null
                        }
                    else null
                val current = CurrentUser.accountUUID.value

                when {
                    uuid != current -> {
                        if (uuid == null) return@addOnDestinationChangedListener

                        runBlocking { CurrentUser.accountUUID.send(uuid) }

                        //starts loading database as soon as possible
                        GlobalScope.launch(Dispatchers.Default) {
                            APIBase.getDatabase(activity.applicationContext, uuid)
                        }
                    }
                    uuid == null && current == null -> {
                        throw IllegalArgumentException("No user uuid given")
                    }
                    else -> {
                    }
                }
            }
        }
        observeForControllerGraphChanges(controller, GlobalScope, root = {
            CurrentUser.accountUUID.send(null)
        })
    }

}