package cz.lastaapps.bakalariextension.tools

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


/**
 * Checks connection to school server, if there is no URL available, to www.google.com*/
class CheckInternet {
    companion object {
        private val TAG = "${CheckInternet::class.java.simpleName}"

        fun check(canBeGoogle: Boolean = true): Boolean {
            val cm = App.appContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = cm.activeNetworkInfo
            return if (activeNetwork != null && activeNetwork.isConnected) {
                try {
                    var stringUrl = ConnMgr.getRawUrl()
                    if (stringUrl == "") {
                        if (canBeGoogle) {
                            Log.i(TAG, "No school url set, checking at least google")
                            stringUrl = "https://www.google.com"
                        } else return false
                    }
                    Log.i(TAG, "Checking connection to $stringUrl")

                    val url = URL(stringUrl)
                    val urlc: HttpURLConnection = url.openConnection() as HttpURLConnection
                    urlc.setRequestProperty("User-Agent", "test")
                    urlc.setRequestProperty("Connection", "close")
                    urlc.connectTimeout = 1000 // mTimeout is in seconds
                    urlc.connect()

                    urlc.responseCode == 200
                } catch (e: IOException) {
                    Log.i("TAG", "Error checking internet connection", e)
                    false
                }
            } else false

            /*return try {
                var stringUrl = ConnMgr.getRawUrl()
                if (stringUrl == "") {
                    if (canBeGoogle) {
                        println("No school url set, checking at least google")
                        stringUrl = "https://www.google.com"
                    } else return false
                }
                Log.i(TAG, "Checking connection to $stringUrl")
                val url = URL(stringUrl)
                val urlConnection = url.openConnection()
                urlConnection.connectTimeout = 5000
                val input = urlConnection.getInputStream()

                val data = input.read()
                input.close()
                Log.i(TAG, "Server working")

                //some data was returned, so it should be working
                data >= 0
            } catch (e: Exception) {
                Log.i(TAG, "Server down")
                e.printStackTrace()
                false
            }*/
        }

        fun canUseInternet(): Boolean {
            return if (SettingsActivity.getSP()
                    .getBoolean(SettingsActivity.MOBILE_DATA, true)) {
                true
            } else {
                connectedMobileData()
            }
        }

        private fun connectedMobileData(): Boolean {

            val cm = App.appContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.isActiveNetworkMetered

            // Get connect mangaer
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