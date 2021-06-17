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

package cz.lastaapps.bakalari.app.ui.start.loading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.api.repo.core.APIRepo
import cz.lastaapps.bakalari.api.repo.user.userRepository
import cz.lastaapps.bakalari.app.ui.start.version.VersionChecker
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import kotlinx.coroutines.launch
import java.util.*

class LoadingViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext

    companion object {
        private val TAG get() = LoadingViewModel::class.simpleName
    }

    private val accountsDatabase = AccountsDatabase.getDatabase(app)
    private val repo = accountsDatabase.repository

    private var isRunning = false

    private var accountToOpen: UUID? = null

    val navigateAction = MutableLiveData<NavDirections?>(null)

    fun determinateNavigation(uuid: UUID?, autoLaunch: Boolean) {

        if (isRunning) return
        isRunning = true

        viewModelScope.launch {

            navigateAction.postValue(
                when (getAccountsStateDecision(uuid, autoLaunch)) {
                    LoadingResults.UPDATE_VERSION -> LoadingFragmentDirections.actionLoadingToVersionUpdate()
                    LoadingResults.OPEN_ACCOUNT -> doOnStartActions(accountToOpen!!)
                    LoadingResults.OPEN_PROFILE_CHOOSER -> LoadingFragmentDirections.actionLoadingToProfiles()
                    else -> null
                }
            )

            isRunning = false
        }
    }

    private suspend fun getAccountsStateDecision(
        requested: UUID?,
        autoLaunch: Boolean
    ): LoadingResults {

        if (VersionChecker.isUpdateRequired(context)) {
            return LoadingResults.UPDATE_VERSION
        }

        if (requested != null) {
            accountToOpen = requested
            return LoadingResults.OPEN_ACCOUNT
        }

        val auto = repo.getAutoStart()
        return if (auto != null && autoLaunch) {
            accountToOpen = auto.uuid
            LoadingResults.OPEN_ACCOUNT
        } else {
            LoadingResults.OPEN_PROFILE_CHOOSER
        }
    }

    private suspend fun doOnStartActions(uuid: UUID): NavDirections {
        val database = APIBase.getDatabase(context, uuid)!!

        val userRepo = database.userRepository
        if (!userRepo.hasData.value) {

            when (userRepo.refreshDataAndWait()) {
                APIRepo.SUCCEEDED -> {
                }
                APIRepo.FAILED -> {
                    return LoadingFragmentDirections.actionLoadingFailed()
                }
            }
        }

        //user object loaded in the API database
        return LoadingFragmentDirections.actionLoadingToUser(uuid)
    }
}

enum class LoadingResults {
    OPEN_ACCOUNT, OPEN_PROFILE_CHOOSER, UPDATE_VERSION
}
