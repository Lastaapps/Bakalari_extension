package cz.lastaapps.bakalariextension.login

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
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

    }

    lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val vM: LoginViewModel by viewModels()
        viewModel = vM

        townSpinner = findViewById(R.id.town_spinner)
        schoolSpinner = findViewById(R.id.school_spinner)
        urlEdit = findViewById(R.id.url)
        usernameEdit = findViewById(R.id.username)
        passwordEdit = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)

        urlEdit.setText(LoginData.url)
        usernameEdit.setText(LoginData.username)

        //town spinner init, used to select the town of school
        townSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.townIndex = position
                LoadSchools().execute(viewModel.townList[viewModel.townIndex])
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

        if (viewModel.townList.isEmpty()) {
            //shown dialog until towns are loaded
            val loadingDialog = AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(R.string.login_loading)
                .create()

            LoadTowns().execute(loadingDialog)

            loadingDialog.show()
        } else {
            fillTownSpinner()
            fillSchoolSpinner()
        }
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
                        XmlPullParser.START_DOCUMENT -> viewModel.townList = ArrayList()
                        XmlPullParser.START_TAG -> {
                            name = parser.name
                            isInName = name == "name"
                        }
                        XmlPullParser.TEXT -> {
                            if (isInName) {
                                viewModel.townList.add(parser.text.trim())
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
                fillTownSpinner()
            } else {
                Toast.makeText(App.context, R.string.error_no_internet, Toast.LENGTH_LONG)
                    .show()
            }
            dialog.dismiss()
        }
    }

    private fun fillTownSpinner() {
        val adapter =
            ArrayAdapter(this@LoginActivity, android.R.layout.simple_spinner_item, viewModel.townList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        townSpinner.adapter = adapter

        var i = viewModel.townList.indexOf(
            LoginData.town
        )
        if (viewModel.townIndex >= 0)
            i = viewModel.townIndex

        if (0 <= i) {
            townSpinner.setSelection(i)
        } else {
            townSpinner.setSelection(viewModel.townList.size / 2)
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
                        XmlPullParser.START_DOCUMENT -> viewModel.schoolUrlMap = HashMap()
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
                                viewModel.schoolUrlMap[schoolName] = schoolUrl
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
                fillSchoolSpinner()
            } else {
                schoolSpinner.adapter = null
                Toast.makeText(App.context, R.string.error_no_internet, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun fillSchoolSpinner() {
        val adapter = ArrayAdapter(
            this@LoginActivity, android.R.layout.simple_spinner_item,
            ArrayList(viewModel.schoolUrlMap.keys)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        schoolSpinner.adapter = adapter

        var i = adapter.getPosition(
            LoginData.school
        )
        if (viewModel.schoolIndex >= 0)
            i = viewModel.schoolIndex

        if (0 <= i) {
            schoolSpinner.setSelection(i)
        }

        schoolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.schoolIndex = position
                urlEdit.setText(viewModel.schoolUrlMap[schoolSpinner.getItemAtPosition(position).toString()])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                schoolSpinner.adapter = null
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
                        LoginToServer.WRONG_LOGIN -> {
                            Toast.makeText(this@LoginActivity, R.string.error_wrong_login_data, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        dialog.show()
        LoginToServer.execute(data, todo)
    }
}


