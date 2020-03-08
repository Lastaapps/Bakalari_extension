package cz.lastaapps.bakalariextension.login

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cz.lastaapps.bakalariextension.LoadingActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.App
import kotlinx.android.synthetic.main.activity_login.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

/**
 * User interface to login to server
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        private val TAG = LoginActivity::class.java.simpleName

        private lateinit var townSpinner: Spinner
        private lateinit var schoolSpinner: Spinner
        private lateinit var urlEdit: EditText
        private lateinit var usernameEdit: EditText
        private lateinit var passwordEdit: EditText
        private lateinit var loginButton: Button

        lateinit var townList: ArrayList<String>
        lateinit var schoolMap: HashMap<String, String>
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        townSpinner = findViewById(R.id.town_spinner)
        schoolSpinner = findViewById(R.id.school_spinner)
        urlEdit = findViewById(R.id.url)
        usernameEdit = findViewById(R.id.username)
        passwordEdit = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)

        urlEdit.setText(
            LoginData.get(
                LoginData.SP_URL
            )
        )
        usernameEdit.setText(
            LoginData.get(
                LoginData.SP_USERNAME
            )
        )

        //town spinner init, used to select the town of school
        townSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                LoadSchools().execute(townList[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                schoolSpinner.adapter = null
            }
        }

        loginButton.setOnClickListener {
            login()
        }
    }

    override fun onStart() {
        super.onStart()

        //shown dialog until towns are loaded
        val loadingDialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(R.string.login_loading)
            .create()

        LoadTowns().execute(loadingDialog)

        loadingDialog.show()
    }

    /**
     * Loads towns and puts them into spinner
     */
    inner class LoadTowns : AsyncTask<AlertDialog, Unit, Boolean>() {

        private lateinit var dialog: AlertDialog

        override fun doInBackground(vararg args: AlertDialog?): Boolean {
            dialog = args[0]!!
            Log.i(TAG, "Loading towns")

            try {
                val url = URL("https://sluzby.bakalari.cz/api/v1/municipality")
                val urlConnection = url.openConnection()
                val input = urlConnection.getInputStream()

                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(input, null)

                var eventType = parser.eventType
                var isInName = false
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    var name: String
                    when (eventType) {
                        XmlPullParser.START_DOCUMENT -> townList = ArrayList()
                        XmlPullParser.START_TAG -> {
                            name = parser.name
                            isInName = name == "name"
                        }
                        XmlPullParser.TEXT -> {
                            if (isInName) {
                                townList.add(parser.text.trim())
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            isInName = false
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load towns")
                e.printStackTrace()
                return false
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            if (result == true) {
                val adapter =
                    ArrayAdapter(this@LoginActivity, android.R.layout.simple_spinner_item, townList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                townSpinner.adapter = adapter

                val i = townList.indexOf(
                    LoginData.get(
                        LoginData.SP_TOWN
                    )
                )
                if (0 <= i) {
                    townSpinner.setSelection(i)
                } else {
                    townSpinner.setSelection(townList.size / 2)
                }
            } else {
                Toast.makeText(App.appContext(), R.string.error_no_internet, Toast.LENGTH_LONG)
                    .show()
            }
            dialog.dismiss()
        }
    }

    /**
     * Loads schools for the given town and puts them into school spinner
     */
    inner class LoadSchools : AsyncTask<String, Unit, Boolean>() {

        override fun doInBackground(vararg args: String?): Boolean {
            try {
                Log.i(TAG, "Loading schools for town ${args[0]}")
                val stringUrl =
                    "https://sluzby.bakalari.cz/api/v1/municipality/" + Uri.encode(args[0])
                val url = URL(stringUrl)
                val urlConnection = url.openConnection()
                val input = urlConnection.getInputStream()

                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(input, null)

                var eventType = parser.eventType
                var isName = false
                var isUrl = false
                var schoolName = ""
                var schoolUrl = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    var name: String

                    when (eventType) {
                        XmlPullParser.START_DOCUMENT -> schoolMap = HashMap()
                        XmlPullParser.START_TAG -> {
                            name = parser.name
                            when (name) {
                                "schoolInfo" -> {
                                    schoolName = ""
                                    schoolUrl = ""
                                }
                                "name" -> {
                                    isName = true
                                }
                                "schoolUrl" -> {
                                    isUrl = true
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (isName) {
                                schoolName = parser.text.trim()
                            } else if (isUrl) {
                                schoolUrl = parser.text.trim()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "schoolInfo") {
                                schoolMap[schoolName] = schoolUrl
                            }
                            isName = false
                            isUrl = false
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load towns")
                e.printStackTrace()
                return false
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            if (result == true) {
                val adapter = ArrayAdapter(
                    this@LoginActivity, android.R.layout.simple_spinner_item,
                    ArrayList(schoolMap.keys)
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                schoolSpinner.adapter = adapter

                val i = adapter.getPosition(
                    LoginData.get(
                        LoginData.SP_SCHOOL
                    )
                )
                if (0 <= i) {
                    schoolSpinner.setSelection(i)
                }

                schoolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long
                    ) {
                        urlEdit.setText(schoolMap[schoolSpinner.getItemAtPosition(position).toString()])
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        schoolSpinner.adapter = null
                    }
                }
            } else {
                schoolSpinner.adapter = null
                Toast.makeText(App.appContext(), R.string.error_no_internet, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    /**Tries to login to server with given data through LoginToServer class*/
    private fun login() {
        Log.i(TAG, "Trying to log in")
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(R.string.login_connecting)
            .create()

        val data = ArrayList<String>()
        data.add(usernameEdit.text.toString())
        data.add(passwordEdit.text.toString())
        data.add(urlEdit.text.toString())
        data.add(town_spinner.selectedItem.toString())
        data.add(school_spinner.selectedItem.toString())

        Log.i(TAG, "Executing login task")
        val todo =
            //TodoAfterLoginToServer(dialog)
            object: LoginToServer.ToDoAfter {
                override fun run(result: Int) {
                    dialog.dismiss()
                    when (result) {
                        LoginToServer.VALID_TOKEN -> {
                            Toast.makeText(this@LoginActivity, R.string.login_succeeded, Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@LoginActivity, LoadingActivity::class.java))
                            finish()
                        }
                        LoginToServer.NOT_ENOUGH_DATA -> {
                            Toast.makeText(this@LoginActivity, R.string.error_not_enough_data, Toast.LENGTH_LONG).show()
                        }
                        LoginToServer.NO_INTERNET -> {
                            Toast.makeText(this@LoginActivity, R.string.error_server_unavailable, Toast.LENGTH_LONG).show()
                        }
                        LoginToServer.WRONG_USERNAME -> {
                            Toast.makeText(this@LoginActivity, R.string.error_wrong_username, Toast.LENGTH_LONG).show()
                        }
                        LoginToServer.INVALID_TOKEN -> {
                            Toast.makeText(this@LoginActivity, R.string.error_invalid_password, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        dialog.show()
        LoginToServer.execute(data, todo)
    }
}


