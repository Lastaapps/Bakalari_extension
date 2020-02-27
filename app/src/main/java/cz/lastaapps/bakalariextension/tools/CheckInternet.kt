package cz.lastaapps.bakalariextension.tools

import cz.lastaapps.bakalariextension.login.LoginData
import java.net.URL

/**
 * Checks connection to school server, if there is no URL available, to www.google.com*/
class CheckInternet {
    companion object {
        fun check(canBeGoogle: Boolean = true): Boolean {
            return try {
                var stringUrl = LoginData.get(
                    LoginData.SP_URL
                )
                if (stringUrl == "") {
                    if (canBeGoogle) {
                        println("No school url set, checking at least google")
                        stringUrl = "https://www.google.com"
                    } else return false
                }
                val url = URL(stringUrl)
                val urlConnection = url.openConnection()
                val input = urlConnection.getInputStream()

                val data = input.read()
                input.close()

                //some data was returned, so it should be working
                data >= 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
            }
        }
    }
}