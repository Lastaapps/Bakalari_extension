package cz.lastaapps.bakalariextension.login

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import cz.lastaapps.bakalariextension.BootReceiver
import cz.lastaapps.bakalariextension.TimeChangeReceiver
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.api.timetable.TTNotifiService
import cz.lastaapps.bakalariextension.tools.App


class OnLogin {

    companion object {
        fun onLogin(): Boolean {
            Handler(Looper.getMainLooper()).post {
                foreground()
            }

            val success = background()

            if (!success)
                Logout.logout()

            return success
        }

        private fun foreground() {

        }

        private fun background(): Boolean {
            if (!User.login())
                return false

            //enables receivers
            val pm: PackageManager = App.context.packageManager
            val bootReceiver =
                ComponentName(App.context, BootReceiver::class.java)
            pm.setComponentEnabledSetting(
                bootReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            val timeReceiver =
                ComponentName(App.context, TimeChangeReceiver::class.java)
            pm.setComponentEnabledSetting(
                timeReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            TTNotifiService.startService(App.context)

            return true
        }
    }
}