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

package cz.lastaapps.bakalariextension.ui

import cz.lastaapps.bakalariextension.api.user.UserLoader
import cz.lastaapps.bakalariextension.api.user.data.User

/**Holds adn loads data about the user*/
class UserViewModel : RefreshableViewModel<User>(TAG) {

    companion object {
        private val TAG = UserViewModel::class.java.simpleName
    }

    override suspend fun loadServer(): User? {
        return UserLoader.loadFromServer()
    }

    override suspend fun loadStorage(): User? {
        return UserLoader.loadFromStorage()
    }

    override fun shouldReload(): Boolean {
        return UserLoader.shouldReload()
    }
}