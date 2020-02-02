package cz.lastaapps.bakalariextension.ui.login

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.LoadingActivity
import cz.lastaapps.bakalariextension.MainActivity

import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.data.LoginData
import kotlinx.android.synthetic.main.activity_login.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.lang.Exception
import java.net.URL
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LoginActivity : AppCompatActivity() {

    companion object {
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

        /*getSharedPreferences("LOGIN", Context.MODE_PRIVATE).edit().putString("TOWN", "Pelhřimov")
            .apply()
*/

        townSpinner = findViewById<Spinner>(R.id.town_spinner)
        schoolSpinner = findViewById<Spinner>(R.id.school_spinner)
        urlEdit = findViewById<EditText>(R.id.url)
        usernameEdit = findViewById<EditText>(R.id.username)
        passwordEdit = findViewById<EditText>(R.id.password)
        loginButton = findViewById<Button>(R.id.login)

        urlEdit.setText(LoginData.getUrl())
        usernameEdit.setText(LoginData.getUsername())

        townSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                LoadSchools().execute(townList[position])
                //Toast.makeText(this@LoginActivity, "Select", Toast.LENGTH_LONG).show()
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

        val loadingDialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(R.string.login_loading)
            .create()

        LoadTowns().execute(loadingDialog)

        loadingDialog.show()

    }

    inner class LoadTowns : AsyncTask<AlertDialog, Unit, Boolean>() {

        lateinit var dialog: AlertDialog

        override fun doInBackground(vararg args: AlertDialog?): Boolean {
            dialog = args[0]!!

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
                Handler(Looper.getMainLooper()).post {
                }
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

                val i = townList.indexOf(LoginData.getTown())
                if (0 <= i) {
                    townSpinner.setSelection(i)
                } else {
                    townSpinner.setSelection(townList.size / 2)
                }
            } else {
                Toast.makeText(App.appContext(), R.string.error_no_internet, Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }
    }

    inner class LoadSchools : AsyncTask<String, Unit, Boolean>() {

        override fun doInBackground(vararg args: String?): Boolean {
            try {
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
                                schoolMap.put(schoolName, schoolUrl)
                            }
                            isName = false
                            isUrl = false
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
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

                val i = adapter.getPosition(LoginData.getSchool())
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
                Toast.makeText(App.appContext(), R.string.error_no_internet, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun login() {
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

        val mainActivityIntent = Intent(this, LoadingActivity::class.java)
        LoginToServer().execute(data, Runnable {
            dialog.dismiss()
            startActivity(mainActivityIntent)
            finish()
        })

        dialog.show()
    }

}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

