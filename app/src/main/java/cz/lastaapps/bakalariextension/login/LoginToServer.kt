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

package cz.lastaapps.bakalariextension.login

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.tools.CheckInternet


/**
 * Tries to login to server with given data and saves them on success
 */
class LoginToServer(
    val username: String,
    val password: String,
    val url: String,
    val town: String,
    val school: String
) {

    companion object {
        private val TAG = LoginToServer::class.java.simpleName

        /**
         * Just to simplify call from oder classes
         * @param 0 - ArrayList<String> with username, password, is password plain, url, town and school in this oder
         * @param 1 - (optional) Runnable, what to do if login has succeeded
         * @param 2 - (optional, requires param 1) Runnable, what to do if login has failed
         */

        const val VALID_TOKEN = 0
        const val NOT_ENOUGH_DATA = 1
        const val NO_INTERNET = 2
        const val WRONG_LOGIN = 3
    }

    suspend fun run(): Int {
        try {

            //checks if enough data was putted in
            if (url == "" || username == "" || password == "") {
                Log.e(TAG, "Not enough data was entered")
                return NOT_ENOUGH_DATA
            }

            //saves basic data
            if (LoginData.town == "")
                LoginData.town = town
            if (LoginData.school == "")
                LoginData.school = school
            if (LoginData.url == "")
                LoginData.url = url


            //checks for server availability
            if (!CheckInternet.check(false)) {
                Log.e(TAG, "Server is unavailable")
                return NO_INTERNET
            }

            //obtains tokens
            return when (ConnMgr.obtainTokens(username, password)) {
                ConnMgr.LOGIN_OK -> {

                    //downloads default user info
                    if (OnLogin.onLogin(App.context)) {
                        LoginData.saveData(username, url, town, school)
                        VALID_TOKEN
                    } else
                        NO_INTERNET
                }
                ConnMgr.LOGIN_WRONG -> WRONG_LOGIN
                else -> NO_INTERNET
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    App.context,
                    R.string.error_no_internet_or_url,
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            e.printStackTrace()

            return NO_INTERNET
        }
    }
}