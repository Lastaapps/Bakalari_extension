package cz.lastaapps.bakalariextension.login

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.ConnectionManager
import cz.lastaapps.bakalariextension.tools.App
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Tries to login to server with given data and saves them
 */
class LoginToServer : AsyncTask<Any, Unit, String>() {

    companion object {

        /**Just to simplify call from oder classes*/
        fun execute(
            username: String = LoginData.get(
                LoginData.SP_USERNAME
            ),
            password: String = LoginData.getPassword(),
            url: String = LoginData.get(
                LoginData.SP_URL
            ),
            town: String = LoginData.get(
                LoginData.SP_TOWN
            ),
            school: String = LoginData.get(
                LoginData.SP_SCHOOL
            ),
            vararg extras: Any
        ): LoginToServer {

            val data = ArrayList<String>()
            data.add(username)
            data.add(password)
            data.add(url)
            data.add(town)
            data.add(school)
            val task = LoginToServer()
            task.execute(data, extras)
            return task
        }
    }

    lateinit var username: String
    lateinit var password: String
    lateinit var url: String
    lateinit var town: String
    lateinit var school: String

    private lateinit var salt: String
    private lateinit var ikod: String
    private lateinit var typ: String

    //to hide dialogs or save data, run on the Main thread
    private var todoAfterTrue: Runnable? = null
    private var todoAfterFalse: Runnable? = null

    override fun doInBackground(vararg params: Any?): String {
        try {
            val list = params[0] as ArrayList<String>
            username = list[0]
            password = list[1]
            url = list[2]
            town = list[3]
            school = list[4]

            todoAfterTrue =
                if (params.size >= 2) params[1] as Runnable
                else null
            todoAfterFalse =
                if (params.size >= 3) params[1] as Runnable
                else null

            //saves basic data
            if (LoginData.get(
                    LoginData.SP_TOWN
                ) == ""
            ) LoginData.set(
                LoginData.SP_TOWN, town
            )
            if (LoginData.get(
                    LoginData.SP_SCHOOL
                ) == ""
            ) LoginData.set(
                LoginData.SP_SCHOOL, school
            )
            if (LoginData.get(
                    LoginData.SP_URL
                ) == ""
            ) LoginData.set(
                LoginData.SP_URL, url
            )

            if (list.contains("")) {
                return ""
            }

            //loads salt from server for user given
            if (!loadSalt()) {
                return ""
            }

            val token = generateToken(salt, ikod, typ, username, password)

            //if everything succeeded, returns valid token, otherwise nothing
            return if (LoginData.check(token)) token else ""
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

    override fun onPostExecute(token: String?) {
        //informs user
        Toast.makeText(
            App.appContext(), when (token != "") {
                true -> R.string.login_succeed
                else -> R.string.login_failed
            }, Toast.LENGTH_LONG
        ).show()

        //saves valid tokens
        if (token != null && token != "") {
            LoginData.saveData(username, password, url, town, school)
            LoginData.saveToken(token)
            todoAfterTrue?.run()
        } else {
            todoAfterFalse?.run()
        }
    }

    /**
     * @returns generated token
     * */
    private fun generateToken(
        salt: String,
        ikod: String,
        typ: String,
        username: String,
        password: String
    ): String {
        val pwd = hash(salt + ikod + typ + password)
        val date = SimpleDateFormat("YYYYMMdd").format(Date())
        val toHash = "*login*$username*pwd*$pwd*sgn*ANDR$date"
        val hashed = hash(toHash)
        return hashed.replace('\\', '_').replace('/', '_').replace('+', '-')
    }

    /**SHA-512 and BASE64 hash*/
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

        val jObject = ConnectionManager.getLoginCredentials(url, username) ?: return false

        salt = jObject.getString("Salt")
        typ = jObject.getString("Type")
        ikod = jObject.getString("ID")
        jObject.getString("PlainPass")
        jObject.getString("MessageType")

        return !(salt == null || salt == "null" || salt == ""
                || typ == null || typ == "null" || typ == ""
                || ikod == null || ikod == "null" || ikod == "")
    }
}