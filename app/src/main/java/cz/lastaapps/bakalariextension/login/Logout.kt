package cz.lastaapps.bakalariextension.login

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import cz.lastaapps.bakalariextension.BootReceiver
import cz.lastaapps.bakalariextension.TimeChangeReceiver
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.api.timetable.TTNotifiService
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.tools.App

/**Deletes saved token and password, then restarts app*/
class Logout {

    companion object {
        private val TAG = Logout::class.java.simpleName

        fun logout() {
            LoginData.accessToken = ""
            LoginData.refreshToken = ""
            LoginData.tokenExpiration = 0
            User.clear()
            TTStorage.deleteAll()

            //disables receivers
            val pm: PackageManager = App.context.packageManager
            val bootReceiver =
                ComponentName(App.context, BootReceiver::class.java)
            pm.setComponentEnabledSetting(
                bootReceiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            val timeReceiver =
                ComponentName(App.context, TimeChangeReceiver::class.java)
            pm.setComponentEnabledSetting(
                timeReceiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            //stops services
            App.context.stopService(Intent(App.context, TTNotifiService::class.java))

            Log.i(TAG, "Logged out")
        }
    }
}