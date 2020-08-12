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

package cz.lastaapps.bakalariextension.ui.login

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.login.LoginFragment.School
import cz.lastaapps.bakalariextension.ui.login.LoginFragment.Town
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**Contains  and downloads some data for LoginActivity*/
class LoginViewModel : ViewModel() {

    companion object {
        private val TAG = LoginViewModel::class.java.simpleName
    }

    /**List of downloaded schools*/
    val townList = MutableLiveData<List<Town>>()

    val selectedTown = MutableLiveData<Town>()
    val selectedSchool = MutableLiveData<School>()

    private var loadingTowns = false

    /**if there is no school list downloaded yet, downloads one*/
    fun loadTownsIfNeeded() {
        if (townList.value != null) return
        if (loadingTowns) return

        loadingTowns = true

        viewModelScope.launch(Dispatchers.Default) {
            Log.i(TAG, "Loading towns")

            try {
                val xml = withContext(Dispatchers.IO) {
                    //open data stream
                    val url = URL("https://sluzby.bakalari.cz/api/v1/municipality")
                    val urlConnection = url.openConnection()

                    BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                        .readText()
                }

                withContext(Dispatchers.Default) {
                    val root = XmlToJson.Builder(xml)
                        .forceList("/ArrayOfmunicipalityInfo/municipalityInfo")
                        .forceIntegerForPath("/ArrayOfmunicipalityInfo/municipalityInfo/schoolCount")
                        .build()
                        .toJson()!!
                    val list = ArrayList<Town>()

                    val array = root
                        .getJSONObject("ArrayOfmunicipalityInfo")
                        .getJSONArray("municipalityInfo")

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
            }
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
                val xml = withContext(Dispatchers.IO) {
                    //open data stream
                    val url =
                        URL("https://sluzby.bakalari.cz/api/v1/municipality/${Uri.encode(town.name)}")
                    val urlConnection = url.openConnection()
                    urlConnection.readTimeout = 10 * 1000

                    BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                        .readText()
                }

                withContext(Dispatchers.Default) {
                    val root = XmlToJson.Builder(xml)
                        .forceList("/municipality/schools/schoolInfo")
                        .build()
                        .toJson()!!
                    val list = ArrayList<School>()

                    //println(root)

                    val array = root
                        .getJSONObject("municipality")
                        .getJSONObject("schools")
                        .getJSONArray("schoolInfo")

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
}