package cz.lastaapps.bakalariextension.login

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.CheckInternet


/**
 * Tries to login to server with given data and saves them
 */
class LoginToServer : AsyncTask<Any, Unit, Int>() {

    companion object {
        private val TAG = LoginToServer::class.java.simpleName


        fun execute(
            username: String,
            password: String,
            url: String,
            town: String,
            school: String,
            todo: ToDoAfter? = null
        ): LoginToServer {
            val data = ArrayList<String>()
            data.add(username)
            data.add(password)
            data.add(url)
            data.add(town)
            data.add(school)
            return execute(data, todo)
        }

        /**
         * Just to simplify call from oder classes
         * @param 0 - ArrayList<String> with username, password, is password plain, url, town and school in this oder
         * @param 1 - (optional) Runnable, what to do if login has succeeded
         * @param 2 - (optional, requires param 1) Runnable, what to do if login has failed
         */
        fun execute(data: ArrayList<String>, todo: ToDoAfter? = null): LoginToServer {
            val task = LoginToServer()
            task.execute(data, todo)
            return task
        }

        const val VALID_TOKEN = 0
        const val NOT_ENOUGH_DATA = 1
        const val NO_INTERNET = 2
        const val WRONG_LOGIN = 3
    }

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var isPlain: String
    private lateinit var url: String
    private lateinit var town: String
    private lateinit var school: String

    private lateinit var salt: String
    private lateinit var ikod: String
    private lateinit var typ: String

    //to hide dialogs or save data, run on the Main thread
    private var todoAfter: ToDoAfter? = null

    override fun doInBackground(vararg params: Any?): Int {
        try {
            val list = params[0] as ArrayList<String>
            username = list[0]
            password = list[1]
            url = list[2]
            town = list[3]
            school = list[4]

            todoAfter =
                if (params.size >= 2) params[1] as ToDoAfter
                else null

            //saves basic data
            if (LoginData.town == "")
                LoginData.town = town
            if (LoginData.school == "")
                LoginData.school = school
            if (LoginData.url == "")
                LoginData.url = url

            if (list.contains("")) {
                Log.e(TAG, "Not enough data was entered")
                return NOT_ENOUGH_DATA
            }

            //checks for server availability
            if (!CheckInternet.check(false)) {
                Log.e(TAG, "Server is unavailable")
                return NO_INTERNET
            }

            return when (ConnMgr.obtainTokens(username, password)) {
                ConnMgr.LOGIN_OK -> {

                    //downloads default user info
                    if (OnLogin.onLogin())
                        VALID_TOKEN
                    else
                        NO_INTERNET
                }
                ConnMgr.LOGIN_WRONG -> WRONG_LOGIN
                else -> NO_INTERNET
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                        App.context,
                        R.string.error_no_internet_or_url,
                        Toast.LENGTH_LONG
                    )
                    .show()
            }
            e.printStackTrace()
            return NO_INTERNET
        }
    }

    override fun onPostExecute(result: Int) {
        //saves valid tokens
        if (result == VALID_TOKEN) {
            Log.i(TAG, "Saving data")
            LoginData.saveData(username, url, town, school)
        }
        todoAfter?.run(result)
    }

    interface ToDoAfter {
        fun run(result: Int)
    }
}