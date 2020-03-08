package cz.lastaapps.bakalariextension.login

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.CheckInternet
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Tries to login to server with given data and saves them
 */
class LoginToServer : AsyncTask<Any, Unit, String>() {

    companion object {
        private val TAG = "${LoginToServer::class.java.simpleName}"


        /**
         * Just to simplify call from oder classes
         * @param 0 - ArrayList<String> with username, password, url, town and school in this oder
         * @param 1 - (optional) Runnable, what to do if login has succeeded
         * @param 2 - (optional, requires param 1) Runnable, what to do if login has failed
         */
        fun execute(
            username: String = LoginData.get(LoginData.SP_USERNAME),
            password: String = LoginData.getPassword(),
            isPlain: String = "false",
            url: String = LoginData.get(LoginData.SP_URL),
            town: String = LoginData.get(LoginData.SP_TOWN),
            school: String = LoginData.get(LoginData.SP_SCHOOL),
            todo: ToDoAfter? = null
        ): LoginToServer {
            val data = ArrayList<String>()
            data.add(username)
            data.add(password)
            data.add(isPlain)
            data.add(url)
            data.add(town)
            data.add(school)
            return execute(data, todo)
        }

        fun execute(data: ArrayList<String>, todo: ToDoAfter? = null): LoginToServer {
            val task = LoginToServer()
            task.execute(data, todo)
            return task
        }

        const val VALID_TOKEN = 0
        const val NOT_ENOUGH_DATA = 1
        const val NO_INTERNET = 2
        const val WRONG_USERNAME = 3
        const val INVALID_TOKEN = -1
    }

    lateinit var username: String
    lateinit var password: String
    lateinit var isPlain: String
    lateinit var url: String
    lateinit var town: String
    lateinit var school: String

    private lateinit var salt: String
    private lateinit var ikod: String
    private lateinit var typ: String

    //to hide dialogs or save data, run on the Main thread
    private var todoAfter: ToDoAfter? = null
    var result = -1

    override fun doInBackground(vararg params: Any?): String {
        try {
            val list = params[0] as ArrayList<String>
            username = list[0]
            password = list[1]
            isPlain = list[2]
            url = list[3]
            town = list[4]
            school = list[5]

            todoAfter =
                if (params.size >= 2) params[1] as ToDoAfter
                else null

            //saves basic data
            if (LoginData.get(LoginData.SP_TOWN) == "")
                LoginData.set(LoginData.SP_TOWN, town)
            if (LoginData.get(LoginData.SP_SCHOOL) == "")
                LoginData.set(LoginData.SP_SCHOOL, school)
            if (LoginData.get(LoginData.SP_URL) == "")
                LoginData.set(LoginData.SP_URL, url)

            if (list.contains("")) {
                result = NOT_ENOUGH_DATA
                Log.e(TAG, "Not enough data was entered")
                return ""
            }

            //checks for server availability
            if (!CheckInternet.check(false)) {
                result = NO_INTERNET
                Log.e(TAG, "Server is unavailable")
                return ""
            }

            if (isPlain == "true") {
                //loads salt from server for given user
                if (!loadSalt()) {
                    result = WRONG_USERNAME
                    Log.e(TAG, "Failed to obtain salt, wrong username")
                    return ""
                }
                password = generateSemiPassword(salt, ikod, typ, password)
            }

            val token = generateToken(username, password)
            Log.i(TAG, "Checking token $token")

            if (ConnMgr.checkToken(token) != ConnMgr.TOKEN_VALID) {
                result = INVALID_TOKEN
                Log.e(TAG, "Token NOT valid")
                return ""
            }

            //if everything succeeded, returns valid token
            result = VALID_TOKEN
            Log.i(TAG, "Token valid!")

            return token
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                        App.appContext(),
                        R.string.error_no_internet_or_url,
                        Toast.LENGTH_LONG
                    )
                    .show()
            }
            e.printStackTrace()
            return ""
        }
    }

    override fun onPostExecute(token: String) {
        //saves valid tokens
        if (result == VALID_TOKEN) {
            Log.i(TAG, "Saving new token")
            LoginData.saveData(username, password, url, town, school)
            LoginData.setToken(token)
        }
        todoAfter?.run(result)
    }


    private fun generateSemiPassword(
        salt: String,
        ikod: String,
        typ: String,
        password: String
    ): String {
        return hash(salt + ikod + typ + password)
    }

    /**
     * @returns generated token
     */
    private fun generateToken(
        username: String,
        semiPwd: String
    ): String {
        Log.i(TAG, "Generating token from data given")
        val formatter = SimpleDateFormat("YYYYMMdd")
        formatter.timeZone = TimeZone.getTimeZone("GMT")
        val date = formatter.format(Date())
        val toHash = "*login*$username*pwd*$semiPwd*sgn*ANDR$date"
        val hashed = hash(toHash)
        return hashed.replace('\\', '_').replace('/', '_').replace('+', '-')
    }

    /**SHA-512 and Base64 hash*/
    private fun hash(input: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        md.update(input.toByteArray(Charsets.UTF_8))
        val byteData = md.digest()

        return Base64.encodeToString(byteData, Base64.NO_WRAP).trim()
    }

    /**
     * @return Loads data to generate token from server
     */
    private fun loadSalt(): Boolean {

        Log.i(TAG, "Loading salt")
        val jObject = ConnMgr.getLoginCredentials(url, username) ?: return false

        salt = jObject.getString("Salt")
        typ = jObject.getString("Type")
        ikod = jObject.getString("ID")
        jObject.getString("PlainPass")
        jObject.getString("MessageType")

        return !(salt == "null" || salt == ""
                || typ == "null" || typ == ""
                || ikod == "null" || ikod == "")
    }

    interface ToDoAfter {
        fun run(result: Int)
    }
}