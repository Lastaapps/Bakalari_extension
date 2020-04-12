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

package cz.lastaapps.bakalariextension.ui.timetable.normal

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.api.timetable.Timetable
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.*
import org.threeten.bp.ZoneId
import kotlin.math.abs


/**Shows full week timetable and permanent timetable variants*/
class TimetableFragment : Fragment() {

    companion object {
        private val TAG = TimetableFragment::class.java.simpleName
    }

    //root view of the timetable
    private lateinit var root: View

    //row height
    private var height: Int = 0

    //toolbar is hidden in landscape mode
    private var toolbarVisibility = true

    //ViewModel storing all the not orientation related data
    private lateinit var vm: TimetableViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //inits view model holding data
        val v: TimetableViewModel by activity!!.viewModels()
        vm = v
    }

    override fun onStart() {
        super.onStart()

        //hides toolbar in landscape mode
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            (activity as MainActivity).supportActionBar!!.apply {
                toolbarVisibility = isShowing
                hide()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        //shows toolbar if it was shown before fragment was set
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            (activity!! as MainActivity).supportActionBar!!.apply {
                if (toolbarVisibility)
                    show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //inflates views
        root = inflater.inflate(R.layout.fragment_timetable, container, false)

        //checks for height of timetables to make all rows same height
        val tableBox = root.findViewById<ViewGroup>(R.id.table_box)
        tableBox.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                height = tableBox.measuredHeight / 6
                //val width: Int = edge.measuredWidth
                if (height != 0)
                    tableBox.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        //moves in weeks or cycles backward
        root.findViewById<ImageButton>(R.id.previous_week).setOnClickListener {
            if (!vm.isPermanent) {

                vm.dateTime = TimeTools.previousWeek(vm.dateTime)

            } else {

                vm.cycleIndex--
                val week = vm.week
                if (week != null && vm.cycleIndex < 0) {
                    vm.cycleIndex = week.cycles.size - 1
                }
            }

            updateTimetable()
        }
        //moves in weeks or cycles forward
        root.findViewById<ImageButton>(R.id.next_week).setOnClickListener {

            if (!vm.isPermanent) {
                vm.dateTime = TimeTools.nextWeek(vm.dateTime)

            } else {

                vm.cycleIndex++
                val week = vm.week
                if (week != null && vm.cycleIndex > (week.cycles.size - 1)) {
                    vm.cycleIndex = 0
                }
            }

            updateTimetable()
        }

        //changes to permanent or actual timetable
        root.findViewById<ImageButton>(R.id.permanent_switch).setOnClickListener {
            if (vm.isPermanent) {
                vm.cycleIndex = 0
                (it as ImageButton).setImageDrawable(
                    root.context.resources.getDrawable(R.drawable.permanent)
                )

            } else {
                (it as ImageButton).setImageDrawable(
                    root.context.resources.getDrawable(R.drawable.actual)
                )
            }

            vm.isPermanent = !vm.isPermanent
            updateTimetable()
        }

        /**Reloads timetable from server*/
        root.findViewById<ImageButton>(R.id.reload).setOnClickListener {
            updateTimetable(true)
        }

        /**navigates back to today if user is somewhere in a future or a history*/
        root.findViewById<ImageButton>(R.id.home).setOnClickListener {
            vm.dateTime = TimeTools.monday
            it.visibility = View.GONE
            updateTimetable()
        }

        updateTimetable()

        return root
    }

    /**Updates timetable for new week / cycle*/
    private fun updateTimetable(forceReload: Boolean = false) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {

            //finding views
            val progressBar = root.findViewById<ProgressBar>(R.id.progress_bar)
            val errorMessage = root.findViewById<TextView>(R.id.error_message)
            val table = root.findViewById<View>(R.id.table_box)
            val bottomBox = root.findViewById<ViewGroup>(R.id.bottom_box)
            val lastUpdated = root.findViewById<TextView>(R.id.last_updated)

            //setting loading state
            progressBar.visibility = View.VISIBLE
            errorMessage.visibility = View.GONE
            table.visibility = View.INVISIBLE
            bottomBox.visibility = View.INVISIBLE
            bottomBox.isEnabled = false

            //loads timetable and row height
            withContext(Dispatchers.IO) {
                val toLoad =
                    if (!vm.isPermanent)
                        vm.dateTime
                    else
                        TimeTools.PERMANENT

                val week = Timetable.loadTimetable(toLoad, forceReload)
                vm.week = week

                while (height == 0)
                    delay(1)

                //updated UI with downloaded data
                withContext(Dispatchers.Main) {

                    bottomBox.isEnabled = true

                    progressBar.visibility = View.GONE
                    bottomBox.visibility = View.VISIBLE

                    if (week == null) {
                        //failed to load week
                        errorMessage.visibility = View.VISIBLE
                        errorMessage.text = getString(R.string.error_no_timetable_no_internet)
                        lastUpdated.text = ""
                    } else {
                        table.visibility = View.VISIBLE

                        //sets text like Next week, last updated 12:00 30.2.2020
                        lastUpdated.text = lastUpdatedText()

                        val cycle =
                            if (vm.isPermanent) {
                                if (week.cycles.size > 0)
                                    week.cycles[vm.cycleIndex]
                                else
                                    null
                            } else
                                if (week.cycles.size > 0)
                                    week.cycles[0]
                                else
                                    null

                        //creates actual timetable
                        TimetableCreator.createTimetable(
                            root,
                            week,
                            cycle
                        )
                    }
                }
            }
        }
    }

    /**@return text saying what week is shown and when was this timetable downloaded*/
    private fun lastUpdatedText(): String {

        var toReturn =

            if (!vm.isPermanent) {

                //how much has user moved
                var diff = (TimeTools.monday.toEpochSecond() -
                        vm.dateTime.toEpochSecond()).toInt()
                diff /= 60 * 60 * 24 * 7

                //array used for saying how many week forward/backward user has gone
                val dataArray = App.getStringArray(R.array.week_forms)


                //show button whits takes user back to today
                root.findViewById<ImageButton>(R.id.home).visibility = if (diff in -2..2) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

                //2-4 and 5+ because of czech inflection
                when (diff) {
                    in 5..Int.MAX_VALUE -> {
                        String.format(dataArray[4], abs(diff), dataArray[6])
                    }
                    in 2..4 -> {
                        String.format(dataArray[3], abs(diff), dataArray[6])
                    }
                    1 -> {
                        dataArray[2]
                    }
                    0 -> {
                        dataArray[0]
                    }
                    -1 -> {
                        dataArray[1]
                    }
                    in -4..-2 -> {
                        String.format(dataArray[3], abs(diff), dataArray[5])
                    }
                    //in Int.MIN_VALUE..-5
                    else -> {
                        String.format(dataArray[4], abs(diff), dataArray[5])
                    }
                }
            } else
                getString(R.string.permanent)

        try {
            toReturn += ", " + getString(R.string.last_updated) + " " +
                    TimeTools.format(
                        TTStorage.lastUpdated(
                            if (!vm.isPermanent)
                                vm.dateTime
                            else
                                TimeTools.PERMANENT
                        )!!,
                        TimeTools.NORMAL_DATE_TIME,
                        ZoneId.systemDefault()
                    )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return toReturn
    }
}
