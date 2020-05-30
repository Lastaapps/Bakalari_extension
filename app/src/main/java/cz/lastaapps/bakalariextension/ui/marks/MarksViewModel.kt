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

    /**Loaded subjects and their marks*/
    val marks = MutableLiveData<MarksAllSubjects>()

    /**If SwipeRefreshLayouts are refreshing now*/
    val isRefreshing = MutableLiveData(false)

    /**When user refreshes marks with swipe from the top of the screen*/
    fun onRefresh(context: Context) {

        //set state refreshing
        isRefreshing.value = true

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val newMarks = MarksLoader.loadFromServer()

            withContext(Dispatchers.Main) {
                //hides refreshing icon
                isRefreshing.value = false

                //when download failed
                if (newMarks == null) {
                    Toast.makeText(
                        context, R.string.marks_failed_to_load, Toast.LENGTH_LONG
                    ).show()
                    return@withContext
                }

                //updates marks with new value
                marks.value = newMarks
            }
        }
    }

    // ---- Predictor section -----

    //default average
    val average = MutableLiveData("")

    //text of new average
    val newAverage = MutableLiveData("")

    //color of new average
    val newAverageColor = MutableLiveData(ColorStateList.valueOf(App.getColor(android.R.color.primary_text_dark)))

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
