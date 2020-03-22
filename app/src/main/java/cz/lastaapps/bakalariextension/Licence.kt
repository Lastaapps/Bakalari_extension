package cz.lastaapps.bakalariextension

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.tools.App
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class Licence {

    companion object {
        private val TAG = Licence::class.java.simpleName

        private const val SP_KEY = "LICENCE"
        private const val SP_AGREED = "1.0"
        private const val SP_TIME = "1.0_date"

        fun check(): Boolean {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).getBoolean(SP_AGREED, false)
        }

        private fun agreed() {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putBoolean(SP_AGREED, true)
                putLong(SP_TIME, ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli())
                apply()
            }
        }

        fun showDialog(context: Context, run: Runnable) {
            AlertDialog.Builder(context)
                .setCancelable(false)
                .setMessage(R.string.licence_agreement)
                .setPositiveButton(R.string.agree)
                    { _: DialogInterface, _: Int ->
                        agreed()
                        run.run()
                    }
                .setTitle(R.string.licence)
                .create()
                .show()
        }
    }

}
