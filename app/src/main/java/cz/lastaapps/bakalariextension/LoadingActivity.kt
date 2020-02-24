package cz.lastaapps.bakalariextension

import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.FirebaseAnalytics
import cz.lastaapps.bakalariextension.api.Login
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.login.LoginActivity
import cz.lastaapps.bakalariextension.login.LoginToServer
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.Toast

/**Checks if app has been ever started -> license, if user is logged in -> LoginActivity or -> MainActivity*/
class LoadingActivity : AppCompatActivity() {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        //firebase init
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        LoadingTask().execute()
    }

    /**checks for internet, token and loads data or redirects to needed activity*/
    private inner class LoadingTask : AsyncTask<Any, Any, Intent?>() {

        override fun doInBackground(vararg params: Any?): Intent? {

            if (!Licence.check()) {
                Handler(Looper.getMainLooper()).post {
                    Licence.showDialog(this@LoadingActivity, Runnable {
                        finish()
                        startActivity(Intent(this@LoadingActivity, LoadingActivity::class.java))
                    })
                }
                return null
            }

            //checks, if there are at least some data saved
            if (!CheckInternet.check()) {
                Toast.makeText(this@LoadingActivity, R.string.error_no_internet, Toast.LENGTH_LONG).show()
                if (Login.get(Login.NAME) == "") {
                    Handler(Looper.getMainLooper()).post {
                        AlertDialog.Builder(this@LoadingActivity)
                        .setMessage(R.string.error_no_saved_data)
                        .setCancelable(false)
                        .setPositiveButton(R.string.close) { _: DialogInterface, i: Int -> finish() }
                        .create()
                        .show()
                    }
                    return null
                } else {
                    return Intent(this@LoadingActivity, MainActivity::class.java)
                }
            }

            var token = LoginData.getToken()
            return if (token == "") {
                //not logged in
                Intent(this@LoadingActivity, LoginActivity::class.java)
            } else {
                //logged in
                if (!LoginData.check(token)) {
                    LoginToServer.execute()
                    Thread.sleep(1000)
                } else
                    Login.login(token)
                Intent(this@LoadingActivity, MainActivity::class.java)
            }
        }

        override fun onPostExecute(intent: Intent?) {
            if (intent != null) {
                startActivity(intent)
                finish()
            }
        }
    }
}
