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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.HomeworkLoader
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.databinding.FragmentTimetableBinding
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.lastUpdated
import kotlinx.coroutines.*
import kotlin.math.abs


/**Shows full week timetable and permanent timetable variants*/
class TimetableFragment : Fragment() {

    companion object {
        private val TAG = TimetableFragment::class.java.simpleName
    }

    //root view of the timetable
    private lateinit var binding: FragmentTimetableBinding

    //row height
    private var height: Int = 0

    //toolbar is hidden in landscape mode
    private var toolbarVisibility = true

    //ViewModel storing all the not orientation related data
    private lateinit var vm: TimetableViewModel

    private lateinit var scope: CoroutineScope

    /**how many rows does timetable have right now*/
    private var setOnLessons = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //inits view model holding data
        val v: TimetableViewModel by requireActivity().viewModels()
        vm = v

        scope = CoroutineScope(Dispatchers.Main)
    }

    override fun onDestroy() {
        super.onDestroy()

        //cancel running work
        scope.cancel()
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
            (requireActivity() as MainActivity).supportActionBar!!.apply {
                if (toolbarVisibility)
                    show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        //inflates views
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_timetable, container, false)

        //checks for height of timetables to make all rows same height
        binding.apply {


            tableBox.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    height = tableBox.measuredHeight / 6
                    //val width: Int = edge.measuredWidth
                    if (height != 0) {
                        Log.i(TAG, "Height obtained")
                        tableBox.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })

            //moves in weeks or cycles backward
            previousWeek.setOnClickListener {
                Log.i(TAG, "Previous pressed")

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
            nextWeek.setOnClickListener {
                Log.i(TAG, "Next pressed")

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
            permanentSwitch.setOnClickListener {
                if (vm.isPermanent) {
                    Log.i(TAG, "Switching to normal timetable")

                    vm.cycleIndex = 0
                    (it as ImageButton).setImageDrawable(
                        root.context.resources.getDrawable(R.drawable.permanent)
                    )

                } else {
                    Log.i(TAG, "Switching to permanent timetable")

                    (it as ImageButton).setImageDrawable(
                        root.context.resources.getDrawable(R.drawable.actual)
                    )
                }

                vm.isPermanent = !vm.isPermanent
                updateTimetable()
            }

            /**Reloads timetable from server*/
            reload.setOnClickListener {
                Log.i(TAG, "Reload pressed")

                updateTimetable(true)
            }

            /**navigates back to today if user is somewhere in a future or a history*/
            home.setOnClickListener {
                Log.i(TAG, "Home pressed")

                vm.dateTime = TimeTools.monday
                it.visibility = View.GONE
                updateTimetable()
            }
        }

        updateTimetable()

        return binding.root
    }

    /**Updates timetable for new week / cycle*/
    private fun updateTimetable(forceReload: Boolean = false) {

        scope.launch {

            //finding views
            binding.apply {
                //setting loading state
                progressBar.visibility = View.VISIBLE
                errorMessage.visibility = View.GONE
                tableBox.visibility = View.INVISIBLE
                bottomBox.visibility = View.INVISIBLE
                bottomBox.isEnabled = false
            }


            //loads timetable and row height
            withContext(Dispatchers.IO) {
                val toLoad =
                    if (!vm.isPermanent)
                        vm.dateTime
                    else
                        TimeTools.PERMANENT

                val week = TimetableLoader.loadTimetable(toLoad, forceReload)
                vm.week = week

                if (vm.homework == null)
                    vm.homework = HomeworkLoader.loadHomework()

                //waits until view is laid out
                while (height <= 0)
                    delay(1)

                //updated UI with downloaded data
                withContext(Dispatchers.Main) {

                    yield()

                    if (week == null) {
                        //failed to load week
                        binding.apply {
                            errorMessage.visibility = View.VISIBLE
                            errorMessage.text = getString(R.string.error_no_timetable_no_internet)
                            lastUpdated.text = ""
                        }

                    } else {

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

                        yield()

                        val lessons = week.trimFreeMorning().size
                        if (setOnLessons != lessons) {
                            TimetableCreator.prepareTimetable(binding.root, height, lessons)
                            setOnLessons = lessons
                        }

                        yield()

                        //creates actual timetable
                        TimetableCreator.createTimetable(
                            binding.root,
                            week,
                            cycle,
                            vm.homework
                        )

                        yield()

                        binding.apply {
                            //sets text like Next week, last updated 12:00 30.2.2020
                            lastUpdated.text = lastUpdatedText()

                            yield()

                            tableBox.visibility = View.VISIBLE
                        }
                    }

                    binding.apply {
                        progressBar.visibility = View.GONE
                        bottomBox.visibility = View.VISIBLE
                        bottomBox.isEnabled = true
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
                binding.home.visibility = if (diff in -2..2) {
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

        //add when was timetable last updated
        val lastUpdated = TTStorage.lastUpdated(
            if (!vm.isPermanent)
                vm.dateTime
            else
                TimeTools.PERMANENT
        )
        lastUpdated?.let {
            toReturn += ", " + lastUpdated(App.context, it)
        }

        return toReturn
    }
}
