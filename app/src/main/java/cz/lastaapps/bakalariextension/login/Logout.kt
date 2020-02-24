package cz.lastaapps.bakalariextension.login

import android.content.Intent
import cz.lastaapps.bakalariextension.tools.App

/**Deletes saved token and password, then restarts app*/
class Logout {

    companion object {
        fun logout() {
            LoginData.saveToken("")
            LoginData.clearPassword()
            App.appContext().startActivity(Intent(
                App.appContext(), LoginActivity::class.java))
        }
    }
}