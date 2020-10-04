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
import androidx.lifecycle.MutableLiveData
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.CurrentUser
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.MarksRepository
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksPair
import cz.lastaapps.bakalariextension.api.marks.data.MarksPairList
import cz.lastaapps.bakalariextension.ui.RefreshableListViewModel

/**ViewModel common for all mark fragments
 * holds loaded marks and some data for each fragment*/
class MarksViewModel : RefreshableListViewModel<MarksPairList, MarksRepository>(
    TAG,
    CurrentUser.requireDatabase().marksRepository
) {
    companion object {
        private val TAG = MarksViewModel::class.java.simpleName
    }

    val pairs by lazy { repo.getAllPairs().asLiveData() }

    val marks by lazy { repo.getAllMarks().asLiveData() }

    val newMarks by lazy { repo.getNewMarks().asLiveData() }

    val subjects by lazy { repo.getSubjects().asLiveData() }

    fun getSubjectMarks(subjectId: String) = repo.getMarks(subjectId).asLiveData()

    override val data by lazy { pairs }

    fun getPair(subjectId: String) = repo.getPair(subjectId).asLiveData()

    init {
        addEmptyObserver()
    }

    // ------ Predictor section -------
    //default average
    val average = MutableLiveData("")

    //text of new average
    val newAverage = MutableLiveData("")

    //color of new average
    val newAverageColor =
        MutableLiveData(ColorStateList.valueOf(App.getColor(R.color.primary_text_light)))

    /**Which subject was selected in predictor*/
    val predictorSelected = MutableLiveData(0)

    val pairSelected = MutableLiveData<MarksPair>()

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

    override fun emptyText(context: Context): String {
        return context.getString(R.string.marks_no_marks)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.marks_failed_to_load)
    }
}
