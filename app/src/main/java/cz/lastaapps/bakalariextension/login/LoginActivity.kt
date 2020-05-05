/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Bakalari extension.
 *
 *     Bakalari extension is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Bakalari extension is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Bakalari extension.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cz.lastaapps.bakalariextension.login

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.LoadingActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.BaseActivity
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

/**
 * User interface for login to server
 */
class LoginActivity : BaseActivity() {

    companion object {
        private val TAG = LoginActivity::class.java.simpleName
    }

    private lateinit var townSpinner: Spinner
    private lateinit var schoolSpinner: Spinner
    private lateinit var urlEdit: EditText
    private lateinit var usernameEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var loginButton: Button

    /**Contains apps ui data*/
    lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Opening Login Activity")

        setContentView(R.layout.activity_login)

        //gets viewModel
        val vM: LoginViewModel by viewModels()
        viewModel = vM

        //finds views
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
                Log.i(TAG, "Town selected")

                viewModel.townIndex = position
                loadSchools(viewModel.townList[viewModel.townIndex])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                schoolSpinner.adapter = null
            }
        }

        //when enter pressed in password field
        passwordEdit.setOnEditorActionListener {_, _ ,_ ->
            login()
            true
        }

        loginButton.setOnClickListener {
            login()
        }

        //opens settings
        findViewById<ImageButton>(R.id.open_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
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

            //loads towns
            loadTowns(loadingDialog)

            loadingDialog.show()
        } else {
            //data was already loaded, setting them
            fillTownSpinner()
            fillSchoolSpinner()
        }
    }


    /**Loads towns and puts them into spinner*/
    private fun loadTowns(dialog: AlertDialog) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {

            withContext(Dispatchers.IO) {
                Log.i(TAG, "Loading towns")

                val result = try {
                    //open data stream
                    val url = URL("https://sluzby.bakalari.cz/api/v1/municipality")
                    val urlConnection = url.openConnection()
                    val input = urlConnection.getInputStream()

                    //parse init
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(input, null)

                    withContext(Dispatchers.Default) {

                        //parses the data
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
                    }
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load towns")
                    e.printStackTrace()
                    false
                }

                withContext(Dispatchers.Main) {
                    if (result) {
                        //sets up town spinner
                        fillTownSpinner()
                    } else {
                        Toast.makeText(App.context, R.string.error_no_internet, Toast.LENGTH_LONG)
                            .show()
                    }
                    dialog.dismiss()
                }
            }
        }
    }

    /**Puts items from view model into spinner*/
    private fun fillTownSpinner() {
        Log.i(TAG, "Filling town spinner")

        val adapter =
            ArrayAdapter(
                this@LoginActivity,
                android.R.layout.simple_spinner_item,
                viewModel.townList
            )
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

    /**Loads schools for the given town and puts them into school spinner*/
    private fun loadSchools(url: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {

            withContext(Dispatchers.IO) {
                Log.i(TAG, "Loading schools for town $url")

                val result = try {

                    val stringUrl =
                        "https://sluzby.bakalari.cz/api/v1/municipality/" + Uri.encode(url)
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
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load towns")
                    e.printStackTrace()
                    false
                }

                withContext(Dispatchers.Main) {

                    if (result) {
                        fillSchoolSpinner()
                    } else {
                        schoolSpinner.adapter = null
                        Toast.makeText(App.context, R.string.error_no_internet, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    /**Updates school spinner with new data from view model*/
    private fun fillSchoolSpinner() {
        Log.i(TAG, "Filling school spinner with data")

        val adapter = ArrayAdapter(
            this@LoginActivity, android.R.layout.simple_spinner_item,
            ArrayList(viewModel.schoolUrlMap.keys)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        schoolSpinner.adapter = adapter

        //previous selected school name
        var i = adapter.getPosition(
            LoginData.school
        )
        //loads data from view model, if any school has been selected
        if (viewModel.schoolIndex >= 0)
            i = viewModel.schoolIndex

        if (0 <= i) {
            schoolSpinner.setSelection(i)
        }

        //puts url into place
        schoolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.i(TAG, "School selected")
                viewModel.schoolIndex = position
                urlEdit.setText(
                    viewModel.schoolUrlMap[schoolSpinner.getItemAtPosition(position).toString()]
                )
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

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            val loginToServer = LoginToServer(
                usernameEdit.text.toString(),
                passwordEdit.text.toString(),
                urlEdit.text.toString(),
                town_spinner.selectedItem.toString(),
                school_spinner.selectedItem.toString()
            )

            //actual login
            val result = loginToServer.run()

            Log.i(TAG, "Request done with code $result")

            withContext(Dispatchers.Main) {

                //login done
                dialog.dismiss()

                when (result) {
                    //opens MainActivity
                    LoginToServer.VALID_TOKEN -> {
                        Toast.makeText(
                            this@LoginActivity, R.string.login_succeeded, Toast.LENGTH_LONG
                        ).show()
                        startActivity(
                            Intent(this@LoginActivity, LoadingActivity::class.java)
                        )
                        finish()
                    }
                    //url, username and password needed
                    LoginToServer.NOT_ENOUGH_DATA -> {
                        Toast.makeText(
                            this@LoginActivity, R.string.error_not_enough_data, Toast.LENGTH_LONG
                        ).show()
                    }
                    //cannot connect to server
                    LoginToServer.NO_INTERNET -> {
                        Toast.makeText(
                            this@LoginActivity, R.string.error_server_unavailable, Toast.LENGTH_LONG
                        ).show()
                    }
                    //wrong username or password
                    LoginToServer.WRONG_LOGIN -> {
                        Toast.makeText(
                            this@LoginActivity, R.string.error_wrong_login_data, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        //shows dialog until login is finished
        dialog.show()
    }
}


