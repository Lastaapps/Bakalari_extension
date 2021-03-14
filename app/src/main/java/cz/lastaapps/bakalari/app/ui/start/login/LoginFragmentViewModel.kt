/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalari.app.ui.start.login

import android.app.Application
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.App
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**Contains  and downloads some data for LoginActivity*/
class LoginFragmentViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private val TAG = LoginFragmentViewModel::class.java.simpleName

        const val PROGRESS_ZERO = -1
        const val PROGRESS_DONE = -2
        const val PROGRESS_ERROR = -3
    }

    /**application context*/
    private val context = app.applicationContext

    val profilePictureUri = MutableLiveData<Uri>(null)
    val textProfileName = MutableLiveData("")
    val textUrl = MutableLiveData("")
    val textUsername = MutableLiveData("")
    val textPassword = MutableLiveData("")
    val savePassword = MutableLiveData(true)

    val errorProfileName = MutableLiveData("")
    val errorUrl = MutableLiveData("")
    val errorUsername = MutableLiveData("")
    val errorPassword = MutableLiveData("")

    init {
        textProfileName.observeForever {
            errorProfileName.postValue("")
        }
        textUrl.observeForever {
            errorUrl.postValue("")
        }
        textUsername.observeForever {
            errorUsername.postValue("")
        }
        textPassword.observeForever {
            errorPassword.postValue("")
        }
    }

    private var alreadySetWithAccount = false
    private val editedAccount = MutableLiveData<BakalariAccount?>(null)
    val editingMode = MutableLiveData(false)

    fun setUpWithAccount(account: BakalariAccount?) {
        if (alreadySetWithAccount) return
        alreadySetWithAccount = true

        account?.also {
            profilePictureUri.postValue(it.imageUri)
            textProfileName.postValue(it.profileName)
            textUrl.postValue(it.url)
            textUsername.postValue(it.userName)
            //textPassword.postValue(it.savePassword)
            savePassword.postValue(it.savePassword)
        }
        editedAccount.postValue(account)
        editingMode.postValue(account != null)
    }

    val cancelEnabled = MutableLiveData(false)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val repo = AccountsDatabase.getDatabase(context).repository
            cancelEnabled.postValue(repo.getAccountsNumber() > 0)
        }
    }

    val savePasswordEnabled = MutableLiveData(true)

    init {
        //disables save password option in editing mode, when user has only tokens stored
        //and no password - saved password on anything in the password field is required
        val todo: (Any?) -> Unit = { _: Any? ->
            val account = editedAccount.value
            savePasswordEnabled.postValue(
                if (account != null) {
                    if (account.savePassword) {
                        true
                    } else {
                        textPassword.value != ""
                    }
                } else {
                    true
                }
            )
        }
        editedAccount.observeForever(todo)
        textPassword.observeForever(todo)

        //makes save password option unchecked in case of the CheckBox is disabled
        savePasswordEnabled.observeForever {
            val account = editedAccount.value ?: return@observeForever
            if (it == false && textPassword.value == "" && account.password == "") {
                savePassword.postValue(false)
            }
        }
    }

    val townsLoading = MutableLiveData(false)
    val townsError = MutableLiveData(false)

    /**List of downloaded schools*/
    val townList = MutableLiveData<List<Town>>()

    val selectedTown = MutableLiveData<Town>()
    val selectedSchool = MutableLiveData<School>()

    private var loadingTowns = false

    /**if there is no school list downloaded yet, downloads one*/
    private fun loadTownsIfNeeded() {
        if (townList.value != null) return
        if (loadingTowns) return

        loadingTowns = true
        townsLoading.postValue(true)
        townsError.postValue(false)

        viewModelScope.launch(Dispatchers.Default) {
            Log.i(TAG, "Loading towns")

            try {
                val array = JSONArray(withContext(Dispatchers.IO) {
                    //open data stream
                    val url = URL("https://sluzby.bakalari.cz/api/v1/municipality")
                    val urlConnection = url.openConnection() as HttpsURLConnection
                    urlConnection.setRequestProperty("Accept", "application/json")

                    BufferedReader(InputStreamReader(urlConnection.inputStream)).readText()
                })

                withContext(Dispatchers.Default) {

                    val list = ArrayList<Town>()

                    for (i in 0 until array.length()) {
                        val json = array.getJSONObject(i)

                        val name = json.getString("name")
                        val schools = json.getInt("schoolCount")

                        if (name.trim() != "")
                            list.add(Town(name, schools))
                    }

                    townList.postValue(list.sorted())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load towns")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(App.context, R.string.login_loading_failed, Toast.LENGTH_LONG)
                        .show()
                }

                if (townList.value == null)
                    townsError.postValue(true)
            }
            townsLoading.postValue(false)
        }
    }

    /**contains schools loaded for all the towns*/
    private val schoolLists = HashMap<Town, MutableLiveData<List<School>>>()

    /**@return school list for the town given*/
    fun getSchoolList(town: Town): MutableLiveData<List<School>> {
        if (!schoolLists.keys.contains(town)) {
            schoolLists[town] = MutableLiveData<List<School>>().also {
                loadSchools(town, it)
            }
        }

        return schoolLists[town]!!
    }

    /**downloads schools from the server if required*/
    private fun loadSchools(town: Town, data: MutableLiveData<List<School>>) {
        if (town.schools != null) return

        viewModelScope.launch(Dispatchers.Default) {
            Log.i(TAG, "Loading towns")

            try {
                val json = JSONObject(withContext(Dispatchers.IO) {
                    //open data stream
                    val url =
                        URL("https://sluzby.bakalari.cz/api/v1/municipality/${Uri.encode(town.name)}")
                    val urlConnection = url.openConnection() as HttpsURLConnection
                    urlConnection.setRequestProperty("Accept", "application/json")

                    BufferedReader(InputStreamReader(urlConnection.inputStream)).readText()
                })

                withContext(Dispatchers.Default) {
                    val list = ArrayList<School>()

                    val townName = json.getString("name")
                    val array = json.getJSONArray("schools")

                    for (i in 0 until array.length()) {
                        val json = array.getJSONObject(i)

                        val id = json.getString("id")
                        val name = json.getString("name")
                        val schools = json.getString("schoolUrl")

                        if (name.trim() != "")
                            list.add(School(town, id, name, schools))
                    }

                    data.postValue(list.sorted())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load schools")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    Toast.makeText(App.context, R.string.login_loading_failed, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    init {
        townList.observeForever {
            onTownsLoaded()
        }
        selectedTown.observeForever {
            onTownSelected()
        }
        selectedSchool.observeForever {
            onSchoolSelected()
        }
        loadTownsIfNeeded()
    }

    /**selects default school if there isn't any*/
    private fun onTownsLoaded() {
        if (selectedTown.value == null) {

            val savedName = editedAccount.value?.townName ?: ""
            val list = townList.value!!

            if (savedName != "") {
                for (town in list) {
                    if (town.name == savedName) {
                        selectedTown.value = town
                        return
                    }
                }
            }

            //chooses the first town as default
            selectedTown.value = list[0]
        }
    }

    /**executed when town selection changes*/
    private fun onTownSelected() {

        selectedSchool.value = null

        val schoolList = getSchoolList(selectedTown.value!!)
        if (schoolList.value != null) {
            onSchoolLoaded(schoolList.value!!)
        } else {
            schoolList.observeForever { onSchoolLoaded(it) }
        }
    }

    /**executed when the school list changes*/
    private fun onSchoolLoaded(list: List<School>) {
        if (selectedTown.value == list[0].town) {

            val savedName = editedAccount.value?.schoolName ?: ""

            if (savedName != "") {
                for (school in list) {
                    if (school.name == savedName) {
                        selectedSchool.value = school
                        return
                    }
                }
            }

            //chooses the first town as default
            selectedSchool.value = list[0]
        }
    }

    /**executed when school selection changes*/
    private fun onSchoolSelected() {
        val school = selectedSchool.value
        textUrl.postValue(school?.url ?: "")
    }

    fun processImage(pickedImage: Uri) = viewModelScope.launch(Dispatchers.Default) {

        //TODO
        //loads bitmap
        val filePath = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor =
            App.context.contentResolver.query(pickedImage, filePath, null, null, null)
                ?: return@launch
        cursor.moveToFirst()
        val imagePath: String = cursor.getString(cursor.getColumnIndex(filePath[0]))

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeFile(imagePath, options)

        //scales bitmap to save storage
        val maxSize = 256
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > height) {
            bitmap.scale(maxSize, maxSize * height / width)
        } else {
            bitmap.scale(maxSize * width / height, maxSize)
        }

        //saves image to app internal storage
        val cw = ContextWrapper(App.context)
        val directory = cw.getDir("profile_pictures", Context.MODE_PRIVATE)
        // Create imageDir
        var fileName: String
        var file: File
        do {
            fileName = UUID.randomUUID().toString() + ".jpg"
            file = File(directory, fileName)
        } while (file.exists()) //ensures that file doesn't already exist

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            // Use the compress method on the BitMap object to write image to the OutputStream
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        profilePictureUri.value = file.toUri()
    }

}
