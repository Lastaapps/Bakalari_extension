package cz.lastaapps.bakalariextension.api

import android.util.Base64
import android.util.Log
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.CheckInternet
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*

/**
 * Connection manager for API V2 - JSONs
 */
class ConnMgr {

    companion object {
        private val TAG = ConnMgr::class.java.simpleName

        fun serverGet(module: String, token: String = LoginData.getToken()): JSONObject? {
            return try {
                Log.i(TAG, "Loading api module $module")

                val url = URL("${getRawUrl()}/if/2/$module")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.setRequestProperty("Authorization", "Basic ${getV2Token(token)}")
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")

                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                var response = ""
                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                Log.i(TAG, "Read $response")
                JSONObject(response)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }

        fun serverPost(module: String, list: Map<String, String>): JSONObject? {


            return null
        }

        fun getLoginCredentials(urlString: String, username: String): JSONObject? {
            return try {
                Log.i(TAG, "Loading login info on $urlString for $username")
                    val url = URL("${getRawUrl(urlString)}/if/2/gethx/$username")
                val urlConnection = url.openConnection()
                urlConnection.setRequestProperty("Accept", "text/json")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")

                var response = ""
                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                JSONObject(response)
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "failed to load data")
                e.printStackTrace()
                null
            }
        }

        /**
         * @return TOKEN_NO, TOKEN_INVALID, TOKEN_NO_INTERNET, TOKEN_VALID
         */
        const val TOKEN_NO = 0
        const val TOKEN_INVALID = 1
        const val TOKEN_NO_INTERNET = 2
        const val TOKEN_VALID = 3
        const val TOKEN_ODER = -1
        fun checkToken(token: String): Int {
            if (token == "") return TOKEN_NO

            if (!CheckInternet.check(false))
                return TOKEN_NO_INTERNET

            val json = serverGet("login", token) ?: return TOKEN_ODER
            try {
                return if (json.getString("MessageType") == "Login")
                    TOKEN_VALID
                else
                    TOKEN_INVALID
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return TOKEN_ODER
        }

        fun getRawUrl(url: String = LoginData.get(LoginData.SP_URL)): String {
            return url.replace("/login.aspx", "")
        }

        fun getV2Token(token: String): String {
            //TODO
            val v2Token = Base64.encodeToString(
                "ANDR:${token}".toByteArray(),
                Base64.NO_WRAP
            )
            println("Token: $token" )
            println("V2 Token: $v2Token")
            return v2Token
        }

        //https://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post-using-namevaluepair/29561084#29561084

        private fun getPostDataString(params: HashMap<String, String>): String? {
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