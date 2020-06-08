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

package cz.lastaapps.bakalariextension.ui.marks

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.MarksLoader
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.api.marks.data.SubjectMarks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**ViewModel common for all mark fragments
 * holds loaded marks and some data for each fragment*/
class MarksViewModel : ViewModel() {
    companion object {
        private val TAG = MarksViewModel::class.java.simpleName
    }

    /**Loaded subjects and their marks*/
    val marks = MutableLiveData<MarksAllSubjects>()

    /**triggered when attempts to load marks fail*/
    val failObserve = MutableLiveData<Any>()

    /**If SwipeRefreshLayouts are refreshing now*/
    val isRefreshing = MutableLiveData(false)

    /**When user refreshes marks with swipe from the top of the screen*/
    fun onRefresh(context: Context, forceReload: Boolean = false) {

        if (isRefreshing.value == true)
            return

        //set state refreshing
        isRefreshing.value = true

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            Log.i(TAG, "Loading marks")

            var loaded: MarksAllSubjects?

            if (forceReload) {
                loaded = MarksLoader.loadFromServer()
            } else {

                loaded = MarksLoader.loadFromStorage()

                if (MarksLoader.shouldReload() || loaded == null) {
                    loaded?.let {
                        withContext(Dispatchers.Main) {
                            marks.value = it
                        }
                    }

                    loaded = MarksLoader.loadFromServer()
                }
            }

            withContext(Dispatchers.Main) {

                //when download failed
                if (loaded == null) {
                    Log.e(TAG, "Loading failed")
                    Toast.makeText(
                        context, R.string.homework_failed_to_load, Toast.LENGTH_LONG
                    ).show()
                    failObserve.value = Any()
                } else {
                    //updates marks with new value
                    marks.value = loaded
                }

                //hides refreshing icon
                isRefreshing.value = false
            }
        }
    }

    // ---- Predictor section -----

    //default average
    val average = MutableLiveData("")

    //text of new average
    val newAverage = MutableLiveData("")

    //color of new average
    val newAverageColor =
        MutableLiveData(ColorStateList.valueOf(App.getColor(R.color.primary_text_light)))

    /**Which subject was selected in predictor*/
    val predictorSelected = MutableLiveData(0)

    val selectedSubject: SubjectMarks
        get() {
            //if there is no subject, index is -1
            val index = predictorSelected.value!!
            return if (index >= 0) {
                //gets subject for selected index
                marks.value!!.subjects[index]
            } else {
                SubjectMarks.default
            }
        }

    /**marks for subject selected right now in predictor*/
    val subjectMarks: DataIdList<Mark>
        get() {
            //if there is no subject, index is -1
            val index = predictorSelected.value!!
            return if (index >= 0) {
                //gets subject for selected index
                val subject = marks.value!!.subjects[index]
                subject.marks
            } else {
                DataIdList()
            }
        }

    /**Contains predicted marks for all subjects, key is index of subject*/
    private val _predictorMarks = HashMap<Int, DataIdList<Mark>>()

    /**@return marks predicted and cashed (for selected subject)*/
    val predictorMarks: DataIdList<Mark>
        get() {
            val index: Int = predictorSelected.value!!

            //if there is no subject, index = -1
            if (index < 0) {
                return DataIdList()
            }

            //creates new list for subject if it wasn't created before
            if (!_predictorMarks.containsKey(index))
                _predictorMarks[index] = DataIdList()

            //returns list for subject index given
            return _predictorMarks[index]!!
        }

}
