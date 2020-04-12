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

package cz.lastaapps.bakalariextension.tools

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.login.LoginData
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**Methods related to connecting to internet*/
class CheckInternet {
    companion object {
        private val TAG = CheckInternet::class.java.simpleName

        /**
         * Checks connection to school server, if there is no URL available, to www.google.com
         * */
        fun check(canBeGoogle: Boolean = true): Boolean {
            val cm = App.context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            //checks if device is connected (internet don't have to work)
            val activeNetwork = cm.activeNetworkInfo
            return if (activeNetwork != null && activeNetwork.isConnected) {

                try {
                    //at first tries school's url, then google.com
                    var stringUrl = LoginData.url
                    if (stringUrl == "") {
                        if (canBeGoogle) {
                            Log.i(TAG, "No school url set, checking at least google")
                            stringUrl = "https://www.google.com"
                        } else return false
                    }

                    Log.i(TAG, "Checking connection to $stringUrl")

                    //connects to server
                    val url = URL(stringUrl)
                    val urlc: HttpURLConnection = url.openConnection() as HttpURLConnection
                    urlc.setRequestProperty("User-Agent", "test")
                    urlc.setRequestProperty("Connection", "Keep-Alive")
                    urlc.connectTimeout = 2000
                    urlc.connect()

                    //connection succeed
                    urlc.responseCode == 200
                } catch (e: IOException) {
                    Log.i(TAG, "Error checking internet connection", e)
                    false
                }
            } else false
        }

        /**@return if app can obtain data trough internet now*/
        fun canUseInternet(): Boolean {
            return if (Settings(App.context)
                    .getSP()
                    .getBoolean(Settings(App.context).MOBILE_DATA, true)) {
                true
            } else {
                connectedMobileData()
            }
        }

        /**@return if user is connected to metered network*/
        fun connectedMobileData(): Boolean {

            val cm = App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.isActiveNetworkMetered

            // Get connect manager
            /*val connMgr =
                App.appContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // check for wifi
            val wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            // check for mobile data
            val mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)

            return if (wifi.isAvailable) {
                false
            } else !mobile.isAvailable*/
        }
    }
}