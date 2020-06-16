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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.ui.WhatsNew
import cz.lastaapps.bakalariextension.ui.license.LicenseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    companion object {
        private val TAG = MainViewModel::class.java.simpleName

        const val UNKNOWN = 0
        const val LOGGED_IN = 1
        const val SHOW_LICENSE = 2
        const val SHOW_NO_INTERNET = 3
        const val SHOW_LOGIN_ACTIVITY = 4
    }

    private val loadingInProgress = MutableLiveData(false)

    val result = MutableLiveData(UNKNOWN)

    val loggedIn = MutableLiveData(false)

    val launchInitRun = MutableLiveData(false)

    fun reset() {
        loadingInProgress.value = false
        result.value = UNKNOWN
    }

    fun doDecision() {

        if (loadingInProgress.value == true)
            return

        loadingInProgress.value = true

        viewModelScope.launch(Dispatchers.Default) {

            Log.i(TAG, "Making decision")

            //with new version, there can be new settings which need default value
            if (WhatsNew(App.context).shouldShow())
                MySettings(App.context).initSettings(true)

            //executes when user is logged in
            if (LoginData.isLoggedIn() && LicenseActivity.check()) {

                Log.i(TAG, "Launching from saved data")

                withContext(Dispatchers.Main) {
                    result.value = LOGGED_IN
                }

                return@launch
            }

            //checks if user has accepted the license
            if (!LicenseActivity.check()) {

                Log.i(TAG, "License not agreed")

                withContext(Dispatchers.Main) {
                    result.value = SHOW_LICENSE
                }

                return@launch
            }

            Log.i(TAG, "Not logged in yet")

            //check if there is internet connection, so if user can log in
            if (!CheckInternet.check()) {

                Log.i(TAG, "Cannot connect to internet")

                withContext(Dispatchers.Main) {
                    result.value = SHOW_NO_INTERNET
                }
            } else {
                Log.i(TAG, "Internet working, opening login")

                withContext(Dispatchers.Main) {
                    //opens LoginActivity
                    result.value = SHOW_LOGIN_ACTIVITY
                }
            }
        }
    }
}