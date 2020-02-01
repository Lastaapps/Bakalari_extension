package cz.lastaapps.bakalariextension

import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import cz.lastaapps.bakalariextension.apimodules.Login
import cz.lastaapps.bakalariextension.data.LoginData
import cz.lastaapps.bakalariextension.ui.login.LoginActivity

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        LoadingTask().execute()
    }

    private inner class LoadingTask : AsyncTask<Any, Any, Intent>() {

        override fun doInBackground(vararg params: Any?): Intent {
            val token = LoginData.getToken()
            return if (token == "") {
                //not logged in
                Intent(this@LoadingActivity, LoginActivity::class.java)
            } else {
                //logged in
                Login.login(token)
                Intent(this@LoadingActivity, MainActivity::class.java)
            }
        }

        override fun onPostExecute(intent: Intent?) {
            startActivity(intent)
            finish()
        }
    }
}
