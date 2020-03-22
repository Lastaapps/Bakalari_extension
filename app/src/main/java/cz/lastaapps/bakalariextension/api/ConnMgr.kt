package cz.lastaapps.bakalariextension.api

import android.util.Log
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
 * Connection manager for API V3
 */
class ConnMgr {

    companion object {
        private val TAG = ConnMgr::class.java.simpleName

        fun serverGet(module: String): JSONObject? {
            return try {
                Log.i(TAG, "Loading api module GET $module")

                if (isExpired()) {
                    if (!refreshAccessToken()) {
                        throw Exception("Failed to obtain new access token")
                    }
                }

                val url = URL("${getAPIUrl()}/3/$module")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Authorization", "Bearer ${LoginData.accessToken}")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")

                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")
                var response = ""
                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                br.close()

                Log.i(TAG, "Read succeed")
                if (urlConnection.responseCode == 200) {

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

        fun serverPost(module: String, paramMap: Map<String, String>): JSONObject? {
            return try {
                Log.i(TAG, "Loading api module POST $module")

                if (isExpired()) {
                    if (!refreshAccessToken()) {
                        throw Exception("Failed to obtain new access token")
                    }
                }

                val data = getPostDataString(paramMap)

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

                val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
                output.write(data)
                output.flush()

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

        fun obtainTokens(username: String, password: String): Int {
            var json: JSONObject

            try {
                Log.i(TAG, "Obtaining new access token")

                val paramMap = HashMap<String, String>()
                paramMap["client_id"] = "ANDR"
                paramMap["grant_type"] = "password"
                paramMap["username"] = username
                paramMap["password"] = password
                val data = getPostDataString(paramMap)

                val url = URL("${getAPIUrl()}/login")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Connection", "Keep-Alive")
                urlConnection.setRequestProperty("Content-length", "${data.length}")

                val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
                output.write(data)
                output.flush()

                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                if (urlConnection.responseCode == 400)
                    return LOGIN_WRONG

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

                LoginData.accessToken = json.getString("access_token")
                LoginData.refreshToken = json.getString("refresh_token")
                LoginData.tokenExpiration = json.getInt("expires_in").toLong()

                return LOGIN_OK
            } catch (je: JSONException) {
                je.printStackTrace()
                return LOGIN_WRONG
            } catch (e: Exception) {
                e.printStackTrace()
                return LOGIN_NO_INTERNET
            }
        }

        fun refreshAccessToken(refreshToken: String = LoginData.refreshToken): Boolean {
            return try {
                Log.i(TAG, "Obtaining new access token")

                val paramMap = HashMap<String, String>()
                paramMap["client_id"] = "ANDR"
                paramMap["grant_type"] = "refresh_token"
                paramMap["refresh_token"] = refreshToken
                val data = getPostDataString(paramMap)

                val url = URL("${getAPIUrl()}/login")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                urlConnection.setRequestProperty("Connection", "Keep-Alive")
                urlConnection.setRequestProperty("Content-length", "${data.length}")

                val output = OutputStreamWriter(urlConnection.outputStream, "UTF-8")
                output.write(data)
                output.flush()

                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

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

                LoginData.accessToken = json.getString("access_token")
                LoginData.refreshToken = json.getString("refresh_token")
                LoginData.tokenExpiration = json.getInt("expires_in").toLong()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun isExpired(expireDate: Long = LoginData.tokenExpiration): Boolean {
            return System.currentTimeMillis() > expireDate - 5000 //5 sec just to make sure
        }

        fun getRawUrl(url: String = LoginData.url): String {
            return url.replace("/login.aspx", "")
        }

        fun getAPIUrl(url: String = LoginData.url): String {
            return url.replace("/login.aspx", "/api")
        }

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