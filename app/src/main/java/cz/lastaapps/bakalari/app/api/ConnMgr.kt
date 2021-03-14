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

package cz.lastaapps.bakalari.app.api

import android.content.Context
import android.content.Intent
import android.util.Log
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.authentication.TokensAPI
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import kotlin.math.min


/**
 * Connection manager for Bakalari API V3
 */
object ConnMgr {

    private val TAG = ConnMgr::class.java.simpleName

    private val mutex = Mutex()

    /**@return json containing downloaded data of null if connection failed*/
    suspend fun serverGet(
        appContext: Context,
        account: BakalariAccount,
        module: String,
        dataPairs: Map<String, String> = HashMap()
    ): JSONObject? {
        return JSONObject(
            serverGetString(appContext, account, module, dataPairs) ?: return null
        )
    }

    /**@return String containing downloaded data of null if connection failed*/
    suspend fun serverGetString(
        appContext: Context,
        account: BakalariAccount,
        module: String,
        dataPairs: Map<String, String> = HashMap()
    ): String? = withContext(Dispatchers.IO) {
        return@withContext try {

            val data = getGetDataString(dataPairs)

            Log.i(TAG, "Loading api module GET $module$data")

            //tries to obtain new ACCESS token, if the old one is expired
            val accessToken = getValidAccessToken(appContext, account)

            //creates URL connection
            val url = URL("${account.getAPIUrl()}/3/$module$data")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            urlConnection.setRequestProperty("Authorization", "Bearer $accessToken")
            urlConnection.setRequestProperty("Connection", "Keep-Alive")
            urlConnection.connectTimeout = 15 * 1000
            urlConnection.readTimeout = 15 * 1000

            //reads data from stream
            Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

            if (urlConnection.responseCode == 200) {

                readData(urlConnection)
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
     * @return String containing downloaded data of null if connection failed*/
    suspend fun serverPost(
        appContext: Context,
        account: BakalariAccount,
        module: String,
        dataPairs: Map<String, String>
    ): JSONObject? {
        return JSONObject(
            serverPostString(appContext, account, module, dataPairs) ?: return null
        )
    }

    /**Sends post request to server
     * @param dataPairs contains data pairs
     * @return json containing downloaded data of null if connection failed*/
    suspend fun serverPostString(
        appContext: Context,
        account: BakalariAccount,
        module: String,
        dataPairs: Map<String, String>
    ): String? = withContext(Dispatchers.IO) {
        return@withContext try {

            //creates data to be send via POST
            val data = getPostDataString(dataPairs)

            Log.i(TAG, "Loading api module POST $module#$data")

            //tries to obtain new ACCESS token, if the old one is expired
            val accessToken = getValidAccessToken(appContext, account)

            //creates URL connection
            val url = URL("${account.getAPIUrl()}/3/$module")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Authorization", "Bearer $accessToken")
            urlConnection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            urlConnection.setRequestProperty("Accept", "application/json")
            urlConnection.setRequestProperty("Connection", "Keep-Alive")
            urlConnection.setRequestProperty("Content-length", "${data.length}")
            urlConnection.connectTimeout = 15 * 1000
            urlConnection.readTimeout = 15 * 1000

            //sends data
            val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
            output.write(data)
            output.flush()

            //reads data
            Log.i(
                TAG,
                "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}"
            )

            if (urlConnection.responseCode == 200) {

                readData(urlConnection)
            } else {
                Log.e(TAG, "Wrong server response code, connection failed")
                null
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    /**reads data from stream and creates json from them*/
    private suspend fun readData(
        urlConnection: URLConnection
    ): String = withContext(Dispatchers.IO) {

        //downloads data in chunks, so yield() can be called to make Dispatchers.IO
        // usable for other task, like reading from storage or parallel downloads

        //how much data is read before yield() is called
        val readChunk = min(4096, urlConnection.contentLength)

        val br = BufferedReader(InputStreamReader(urlConnection.inputStream), readChunk)
        val builder = java.lang.StringBuilder(urlConnection.contentLength)

        while (true) {
            val array = CharArray(readChunk)

            val read = br.read(array)

            if (read < 0)
                break

            builder.append(array, 0, read)

            yield()
        }

        br.close()

        //read was successful
        Log.i(TAG, "Read succeed ${builder.length / 1024}kb")

        String(builder)
    }


    /**refreshes access token if needed */
    suspend fun getValidAccessToken(appContext: Context, account: BakalariAccount): String {
        mutex.withLock {

            //tries to obtain new ACCESS token, if the old one is expired
            val pair = TokensAPI(appContext).getRefreshedToken(account.uuid)

            when (pair.first) {
                TokensAPI.SUCCESS, TokensAPI.OLD_TOKENS -> return pair.second!!.accessToken
                TokensAPI.INTERNET_ERROR -> {
                    throw IOException("Internet connection")
                }
                TokensAPI.SERVER_ERROR -> {
                    appContext.sendBroadcast(Intent(MainActivity.INVALID_REFRESH_TOKEN))
                    throw Exception("Wrong response code")
                }
                else -> throw IllegalStateException("Something has broken")
            }
        }
    }


    /**converts map to data, which can be used in GET*/
    private fun getGetDataString(params: Map<String, String>): String {
        if (params.isEmpty())
            return ""

        val result = StringBuilder()
        result.append("?")

        var first = true
        for ((key, value) in params.entries) {
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value, "UTF-8"))
        }
        return result.toString()
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