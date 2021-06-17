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

package cz.lastaapps.bakalari.authentication

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Networking(private val context: Context, private val url: String) {

    companion object {
        private val TAG get() = Networking::class.simpleName

        const val LOGIN_WRONG = -1
        const val LOGIN_OK = 1
        const val LOGIN_NO_INTERNET = 0

        const val REFRESH_WRONG_CODE = -1
        const val REFRESH_OK = 1
        const val REFRESH_NO_INTERNET = 0
    }

    /**Initial obtain on login, gets access and refresh tokens at same time*/
    fun obtainTokens(username: String, password: String, dataUpdated: (JSONObject) -> Unit): Int {
        val json: JSONObject

        try {
            Log.i(TAG, "Obtaining new access token")

            //POST data map
            val dataMap = HashMap<String, String>()
            dataMap["client_id"] = "ANDR"
            dataMap["grant_type"] = "password"
            dataMap["username"] = username
            dataMap["password"] = password
            val data = getPostDataString(dataMap)

            //creating URL connection
            val url = URL("${url.toApiUrl()}/login")
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
            dataUpdated(json)

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
    fun refreshAccessToken(refreshToken: String, dataUpdated: (JSONObject) -> Unit): Int {
        return try {
            Log.i(TAG, "Obtaining new access token")

            //POST data
            val paramMap = HashMap<String, String>()
            paramMap["client_id"] = "ANDR"
            paramMap["grant_type"] = "refresh_token"
            paramMap["refresh_token"] = refreshToken
            val data = getPostDataString(paramMap)

            //creates URL connection
            val url = URL("${url.toApiUrl()}/login")
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

            if (urlConnection.responseCode == 200) {

                //reads response
                var response = ""
                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                Log.i(TAG, "Read succeed")

                output.close()
                br.close()

                val json = JSONObject(response)

                //saves data
                dataUpdated(json)

                REFRESH_OK
            } else {

                return if (urlConnection.responseCode == 400) {
                    Log.e(TAG, "Wrong response code, outdated refresh token")
                    REFRESH_WRONG_CODE
                } else {
                    Log.e(TAG, "Unexpected response")
                    REFRESH_NO_INTERNET
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Failed to obtain new tokens")
            REFRESH_NO_INTERNET
        }
    }

    private fun String.toApiUrl() = "$this/api"

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