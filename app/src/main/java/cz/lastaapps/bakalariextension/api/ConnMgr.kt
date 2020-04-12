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

package cz.lastaapps.bakalariextension.api

import android.app.backup.BackupManager
import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.login.LoginData
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Connection manager for Bakalari API V3
 */
class ConnMgr {

    companion object {
        private val TAG = ConnMgr::class.java.simpleName

        /**@return json containing downloaded data of null if connection failed*/
        fun serverGet(module: String): JSONObject? {
            return try {
                Log.i(TAG, "Loading api module GET $module")

                //tries to obtain new ACCESS token, if the old one is expired
                if (isExpired()) {
                    if (!refreshAccessToken()) {
                        throw Exception("Failed to obtain new access token")
                    }
                }

                //creates URL connection
                val url = URL("${getAPIUrl()}/3/$module")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Authorization", "Bearer ${LoginData.accessToken}")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")

                //reads data from stream
                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                if (urlConnection.responseCode == 200) {

                    var response = ""
                    var line: String?
                    val br = BufferedReader(InputStreamReader(urlConnection.inputStream))

                    while (br.readLine().also { line = it } != null) {
                        response += line
                    }
                    br.close()

                    //read was successful
                    Log.i(TAG, "Read succeed")

                    JSONObject(response)
                } else {

                    Log.e(TAG, "Wrong server response code, connection failed")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**Sends post request to server
         * @param dataPairs contains data pairs
         * @return json containing downloaded data of null if connection failed*/
        fun serverPost(module: String, dataPairs: Map<String, String>): JSONObject? {
            return try {
                Log.i(TAG, "Loading api module POST $module")

                //tries to obtain new ACCESS token, if the old one is expired
                if (isExpired()) {
                    if (!refreshAccessToken()) {
                        throw Exception("Failed to obtain new access token")
                    }
                }

                //creates data to be send via POST
                val data = getPostDataString(dataPairs)

                //creates URL connection
                val url = URL("${getAPIUrl()}/3/$module")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Authorization", "Bearer ${LoginData.accessToken}")
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Host", getRawUrl())
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")
                urlConnection.setRequestProperty("Content-length", "${data.length}")

                //sends data
                val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
                output.write(data)
                output.flush()

                //reads data
                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                if (urlConnection.responseCode == 200) {
                    var response = ""
                    var line: String?
                    val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                    while (br.readLine().also { line = it } != null) {
                        response += line
                    }

                    Log.i(TAG, "Read succeed")

                    JSONObject(response)
                } else {
                    Log.e(TAG, "Wrong server response code, connection failed")
                    null
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }

        const val LOGIN_WRONG = -1
        const val LOGIN_OK = 1
        const val LOGIN_NO_INTERNET = 0

        /**Initial obtain on login, gets access and refresh tokens at same time*/
        fun obtainTokens(username: String, password: String): Int {
            var json: JSONObject

            try {
                Log.i(TAG, "Obtaining new access token")

                //POST data map
                val paramMap = HashMap<String, String>()
                paramMap["client_id"] = "ANDR"
                paramMap["grant_type"] = "password"
                paramMap["username"] = username
                paramMap["password"] = password
                val data = getPostDataString(paramMap)

                //creating URL connection
                val url = URL("${getAPIUrl()}/login")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Connection", "Keep-Alive")
                urlConnection.setRequestProperty("Content-length", "${data.length}")

                //sending data
                val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
                output.write(data)
                output.flush()

                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                //wrong login info
                if (urlConnection.responseCode != 200) {
                    return LOGIN_WRONG
                }

                var response = ""
                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                output.close()
                br.close()

                Log.i(TAG, "Read succeed")

                json = JSONObject(response)

                //saves data
                LoginData.accessToken = json.getString("access_token")
                LoginData.refreshToken = json.getString("refresh_token")
                LoginData.tokenExpiration = json.getInt("expires_in").toLong()

                //notifies that backup should be made
                BackupManager.dataChanged(App.context.packageName)

                //success
                return LOGIN_OK
            } catch (je: JSONException) {
                je.printStackTrace()
                //error was returned, so json cannot be read properly
                return LOGIN_WRONG
            } catch (e: Exception) {
                e.printStackTrace()
                //connection error
                return LOGIN_NO_INTERNET
            }
        }

        /**Refreshes the access token*/
        fun refreshAccessToken(refreshToken: String = LoginData.refreshToken): Boolean {
            return try {
                Log.i(TAG, "Obtaining new access token")

                //POST data
                val paramMap = HashMap<String, String>()
                paramMap["client_id"] = "ANDR"
                paramMap["grant_type"] = "refresh_token"
                paramMap["refresh_token"] = refreshToken
                val data = getPostDataString(paramMap)

                //creates URL connection
                val url = URL("${getAPIUrl()}/login")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Connection", "Keep-Alive")
                urlConnection.setRequestProperty("Content-length", "${data.length}")

                //sends data
                val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
                output.write(data)
                output.flush()

                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                //failed, app is broken...
                if (urlConnection.responseCode != 200) {
                    throw java.lang.Exception("Wrong response code")
                }

                //reads response
                var response = ""
                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                output.close()
                br.close()

                Log.i(TAG, "Read succeed")

                val json = JSONObject(response)

                //saves data
                LoginData.accessToken = json.getString("access_token")
                LoginData.refreshToken = json.getString("refresh_token")
                LoginData.tokenExpiration = json.getInt("expires_in").toLong()

                //notifies that backup should be made
                BackupManager.dataChanged(App.context.packageName)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Failed to obtain new tokens")
                false
            }
        }

        /**@return if refreshing access token is necessary*/
        fun isExpired(expireDate: Long = LoginData.tokenExpiration): Boolean {
            return System.currentTimeMillis() > expireDate - 5000 //5 sec just to make sure
        }

        /**@return url in format example www.example.com */
        fun getRawUrl(url: String = LoginData.url): String {
            return url.replace("/login.aspx", "")
        }

        /**@return url in format www.example.com/api */
        fun getAPIUrl(url: String = LoginData.url): String {
            return url.replace("/login.aspx", "/api")
        }

        /**converts map to data, which can be used in POST*/
        private fun getPostDataString(params: Map<String, String>): String {
            val result = StringBuilder()
            var first = true
            for ((key, value) in params.entries) {
                if (first) first = false else result.append("&")
                result.append(URLEncoder.encode(key, "UTF-8"))
                result.append("=")
                result.append(URLEncoder.encode(value, "UTF-8"))
            }
            return result.toString()
        }
    }
}