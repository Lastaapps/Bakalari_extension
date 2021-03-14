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

import android.content.Context
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.user.UserRepository
import cz.lastaapps.bakalari.app.api.user.data.User
import cz.lastaapps.bakalari.app.ui.uitools.RefreshableDataViewModel
import kotlinx.coroutines.flow.filterNotNull

/**Holds adn loads data about the user*/
class UserViewModel : RefreshableDataViewModel<User, UserRepository>(
    TAG,
    CurrentUser.requireDatabase().userRepository
) {

    companion object {
        private val TAG = UserViewModel::class.java.simpleName
    }

    override val data = repo.getUser().filterNotNull().asLiveData()

    override fun failedText(context: Context): String =
        context.getString(R.string.user_failed_to_load)

}