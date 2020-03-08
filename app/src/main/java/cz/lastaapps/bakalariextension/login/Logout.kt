package cz.lastaapps.bakalariextension.login

import android.util.Log
import cz.lastaapps.bakalariextension.api.Login

/**Deletes saved token and password, then restarts app*/
class Logout {

    companion object {
        private val TAG = "${Logout::class.java.simpleName}"

        fun logout() {
            LoginData.setToken("")
            LoginData.clearPassword()
            Login.clear()
            Log.i(TAG, "Logged out")
        }
    }
}