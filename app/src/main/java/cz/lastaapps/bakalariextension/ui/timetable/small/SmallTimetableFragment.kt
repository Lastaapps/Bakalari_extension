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

package cz.lastaapps.bakalariextension.ui.timetable.small

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.Timetable
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime

class SmallTimetableFragment : Fragment {

    companion object {
        private val TAG = SmallTimetableFragment::class.java.simpleName
    }

    private lateinit var view: SmallTimetableView
    private lateinit var model: STViewModel
    private var _date: ZonedDateTime? = null

    constructor(): super()

    constructor(date: ZonedDateTime): super() {
        _date = date
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val model: STViewModel by activityViewModels()
        this.model = model

        if (model.date == null)
            model.date = _date ?: TimeTools.now

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view for SmallTimetable fragment")

        view =
            SmallTimetableView(
                inflater.context
            )

        return view
    }

    override fun onStart() {
        super.onStart()

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {

            //shows progress bar while loading
            view.setLoading()

            withContext(Dispatchers.Default) {

                //loads week
                val date = model.date!!
                val week = Timetable.loadTimetable(date)

                withContext(Dispatchers.Main) {

                    //updates data
                    if (week == null) {
                        view.setError(resources.getString(R.string.error_no_timetable_no_internet))
                    } else {
                        val day = week.getDay(date)
                        if (day == null) {
                            view.setError(resources.getString(R.string.error_no_timetable_for_today))
                        } else {
                            view.updateTimetable(week, day)
                        }
                    }
                }
            }
        }
    }

    //ViewModel holding date
    class STViewModel: ViewModel() {
        var date: ZonedDateTime? = null
    }
}
