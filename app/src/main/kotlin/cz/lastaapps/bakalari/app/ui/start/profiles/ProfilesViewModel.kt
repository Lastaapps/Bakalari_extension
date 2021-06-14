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

package cz.lastaapps.bakalari.app.ui.start.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfilesViewModel(private val app: Application) : AndroidViewModel(app) {

    val database = AccountsDatabase.getDatabase(app.applicationContext)
    val repo = database.repository

    val accounts = repo.getAllObservable().asLiveData(viewModelScope.coroutineContext)

    val editingMode = MutableLiveData(false)

    fun autoLaunchChanged(newState: Boolean, account: BakalariAccount) {
        viewModelScope.launch(Dispatchers.Default) {
            val newAccount = account.copy(autoLaunch = newState)
            repo.updateAutoLaunch(newAccount)
        }
    }
}