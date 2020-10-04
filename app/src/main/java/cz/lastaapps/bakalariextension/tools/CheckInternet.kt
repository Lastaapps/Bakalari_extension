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
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.ui.login.LoginData
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
        fun check(canBeGoogle: Boolean = true, mustBeGoogle: Boolean = false): Boolean {
            val cm = App.context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            //checks if device is connected (internet don't have to work)
            val activeNetwork = cm.activeNetworkInfo
            return if (isConnected(App.context)) {

                try {
                    //at first tries school's url, then google.com
                    val stringUrl = if (LoginData.url == "" || mustBeGoogle) {
                        if (canBeGoogle || mustBeGoogle) {
                            Log.i(TAG, "No school url set, checking at least google")
                            "https://www.google.com"
                        } else return false
                    } else {
                        ConnMgr.getAPIUrl()
                    }

                    Log.i(TAG, "Checking connection to $stringUrl")

                    //connects to server
                    val url = URL(stringUrl)
                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.setRequestProperty("Connection", "Keep-Alive")
                    connection.connectTimeout = 7000
                    connection.readTimeout = 3000
                    connection.connect()

                    Log.i(TAG, "Connection state: ${connection.responseCode}")

                    connection.disconnect()

                    true
                } catch (e: IOException) {
                    Log.e(TAG, "Error checking internet connection", e)
                    false
                }
            } else false
        }

        private fun isConnected(context: Context): Boolean {
            var result = false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork ?: return false
                val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
                result = when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                connectivityManager.run {
                    connectivityManager.activeNetworkInfo?.run {
                        result = when (type) {
                            ConnectivityManager.TYPE_WIFI -> true
                            ConnectivityManager.TYPE_MOBILE -> true
                            ConnectivityManager.TYPE_ETHERNET -> true
                            else -> false
                        }

                    }
                }
            }

            return result
        }

        /**@return if app can obtain data trough internet now*/
        fun canUseInternet(): Boolean {
            return if (MySettings(App.context)
                    .getSP()
                    .getBoolean(MySettings(App.context).MOBILE_DATA, true)
            ) {
                true
            } else {
                connectedMobileData()
            }.also { Log.i(TAG, "Can use internet: $it") }
        }

        /**@return if user is connected to metered network*/
        fun connectedMobileData(): Boolean {

            val cm =
                App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.isActiveNetworkMetered.also {
                Log.i(TAG, "Connected to mobile data: $it")
            }

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