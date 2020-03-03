package cz.lastaapps.bakalariextension

import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.FirebaseAnalytics
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.Login
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.login.LoginActivity
import cz.lastaapps.bakalariextension.login.LoginToServer
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MyToast
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity
import java.security.Permission

/**Checks if app has been ever started -> license, if user is logged in -> LoginActivity or -> MainActivity*/
class LoadingActivity : AppCompatActivity() {

    companion object {
        private val TAG = "${LoadingActivity::class.java.simpleName}"
    }

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //resets settings, for testing
        //SettingsActivity.getSP().edit().clear().apply()

        SettingsActivity.initSettings()
        SettingsActivity.updateLanguage(this)
        SettingsActivity.updateDarkTheme()

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
                        Handler(Looper.getMainLooper()).postDelayed(
                            { LoadingTask().execute() },
                            100
                        )
                    })
                }
                return null
            }

            //checks, if there are at least some data saved
            if (!CheckInternet.check()) {
                Log.i(TAG, "Cannot connect to server")
                MyToast.makeText(
                    this@LoadingActivity,
                    R.string.error_no_internet,
                    MyToast.LENGTH_LONG
                ).show()
                return if (Login.get(Login.NAME) == "") {
                    Log.i(TAG, "No data saved yet")
                    Handler(Looper.getMainLooper()).post {
                        AlertDialog.Builder(this@LoadingActivity)
                            .setMessage(R.string.error_no_saved_data)
                            .setCancelable(false)
                            .setPositiveButton(R.string.close) { _: DialogInterface, _: Int -> finish() }
                            .create()
                            .show()
                    }
                    null
                } else {
                    Log.i(TAG, "Launching from saved data")
                    Intent(this@LoadingActivity, MainActivity::class.java)
                }
            }
            Log.i(TAG, "Internet is working")

            val token = LoginData.getToken()
            return if (token == "") {
                //not logged in
                Log.i(TAG, "No token generated yet")
                Intent(this@LoadingActivity, LoginActivity::class.java)
            } else {
                //logged in
                if (ConnMgr.checkToken(token) != ConnMgr.TOKEN_VALID) {
                    Log.e(TAG, "Token is outdated, obtaining new one")
                    LoginToServer.execute(todo = object: LoginToServer.ToDoAfter {
                        override fun run(result: Int) {
                            when (result) {
                                LoginToServer.VALID_TOKEN, LoginToServer.NO_INTERNET -> {
                                    Handler(Looper.getMainLooper()).postDelayed(
                                        { LoadingTask().execute() },
                                        100
                                    )}
                                LoginToServer.NOT_ENOUGH_DATA,
                                LoginToServer.WRONG_USERNAME,
                                LoginToServer.INVALID_TOKEN -> {
                                    MyToast.makeText(App.appContext(), R.string.error_login_failed, MyToast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })
                    return null
                } else
                    Login.login()
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
