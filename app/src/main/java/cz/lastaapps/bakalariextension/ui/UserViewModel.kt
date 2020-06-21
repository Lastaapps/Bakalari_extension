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

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.user.UserLoader
import cz.lastaapps.bakalariextension.api.user.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**Holds adn loads data about the user*/
class UserViewModel : RefreshableViewModel<User>() {

    override fun onRefresh(force: Boolean) {

        if (isRefreshing.value!!)
            return

        isRefreshing.value = true

        viewModelScope.launch(Dispatchers.Default) {

            var user = UserLoader.loadFromStorage()

            if (user == null || force) {
                user?.let {
                    withContext(Dispatchers.Main) {
                        data.value = it
                    }
                }

                user = UserLoader.loadFromServer()
            }

            withContext(Dispatchers.Main) {

                failed.value = false
                isEmpty.value = false

                if (user == null) {
                    if (data.value == null) {
                        failed.value = true
                    }
                    Toast.makeText(App.context, R.string.user_failed_to_load, Toast.LENGTH_LONG)
                        .show()
                } else {
                    data.value = user
                }

                isRefreshing.value = false
            }
        }
    }
}