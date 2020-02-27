package cz.lastaapps.bakalariextension.api

import android.util.Base64
import cz.lastaapps.bakalariextension.login.LoginData
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.util.HashMap

/**
 * Connection manager for API V2 - JSONs
 */
class ConnectionManager {
    companion object {

        fun serverGet(module: String, token: String = LoginData.getToken()): JSONObject? {
            var response = ""
            return try {
                val url = URL("${getRawUrl()}/if/2/$module")
                val urlConnection = url.openConnection()
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.setRequestProperty("Authorization", "Basic ${getV2Token(token)}")
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")

                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

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
            var response = ""
            return try {
                val url = URL("${getRawUrl(urlString)}/if/2/gethx/$username")
                val urlConnection = url.openConnection()
                urlConnection.setRequestProperty("Accept", "text/json")
                urlConnection.setRequestProperty("Connection", "Keep-Alive")

                var line: String?
                val br = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                while (br.readLine().also { line = it } != null) {
                    response += line
                }

                JSONObject(response)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }

        fun getRawUrl(url: String = LoginData.get(LoginData.SP_URL)): String {
            return url.replace("/login.aspx", "")
        }

        fun getV2Token(token: String): String {
            return Base64.encodeToString(
                "ANDR:${token}".toByteArray(),
                Base64.NO_WRAP
            )
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