package cz.lastaapps.bakalariextension

import android.os.Handler
import android.os.Looper
import android.widget.Toast

class WrongLogin: Exception() {

    fun showToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(App.appContext(), R.string.error_wrong_login, Toast.LENGTH_LONG).show()
        }
    }
}