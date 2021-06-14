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

package cz.lastaapps.bakalari.app.ui.user.timetable.normal

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.api.entity.timetable.Week
import cz.lastaapps.bakalari.api.repo.timetable.WebTimetableDate
import cz.lastaapps.bakalari.api.repo.timetable.WebTimetableType
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentTimetableBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.homework.HmwViewModel
import cz.lastaapps.bakalari.app.ui.user.timetable.TimetableMainViewModel
import cz.lastaapps.bakalari.app.ui.user.timetable.TimetableViewModel
import cz.lastaapps.bakalari.platform.App
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCalendar
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import cz.lastaapps.bakalari.tools.TimeTools.toMonday
import cz.lastaapps.bakalari.tools.ui.isDarkTheme
import cz.lastaapps.bakalari.tools.ui.lastUpdated
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs


/**Shows full week timetable and permanent timetable variants*/
class TimetableFragment : Fragment() {

    companion object {
        private val TAG = TimetableFragment::class.java.simpleName
    }

    private var updatingJob: Job? = null

    //holds current state of timetable views, prevents unnecessary updates
    private var layoutForWeek: Week? = null
    private var layoutForHeight: Int = -1
    private var layoutForCycle: SimpleData? = null

    //root view of the timetable
    private lateinit var binding: FragmentTimetableBinding

    //row height
    private var totalHeight = 0
    private var viewsLayout = false

    //toolbar is hidden in landscape mode
    private var toolbarVisibility = true

    //ViewModel storing all the not orientation related data
    private val mainVM: TimetableMainViewModel by accountsViewModels()
    private lateinit var currentVM: TimetableViewModel
    private val homeworkViewModel: HmwViewModel by accountsViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
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
    ): View {

        Log.i(TAG, "Creating view")

        val placeholder =
            inflater.inflate(R.layout.fragment_timetable_placeholder, container, false) as ViewGroup

        //inflating off the UI thread
        lifecycleScope.launch(Dispatchers.Default) {

            mainVM.waitForInitialization(lifecycle)

            val root = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_timetable, placeholder, false)
            root.layoutParams = root.layoutParams.also {
                it.width = ViewGroup.LayoutParams.MATCH_PARENT
                it.height = ViewGroup.LayoutParams.MATCH_PARENT
            }

            Log.i(TAG, "Real view inflated")

            withContext(Dispatchers.Main) {
                initView(root)
                yield()
                placeholder.addView(root)
                launch {
                    viewsLayout = true
                    updateTimetable()
                }
                yield()
                placeholder.findViewById<ProgressBar>(R.id.loading).visibility = View.GONE
            }
        }

        prepareDefaultViewModel()

        return placeholder
    }

    @UiThread
    private suspend fun initView(root: View) {

        //inflates views
        binding = DataBindingUtil.bind(root)!!
        binding.setLifecycleOwner { lifecycle }
        binding.mainViewModel = mainVM
        onSelectionChanged()
        yield()

        //checks for height of timetables to make all rows same height
        binding.apply {

            tableLayout.viewTreeObserver.addOnGlobalLayoutListener {
                //val width: Int = edge.measuredWidth.also{ width -> }
                tableLayout.measuredHeight.also { height ->
                    if (height != 0 && totalHeight != height) {
                        Log.i(TAG, "Height obtained $height")
                        totalHeight = height
                        updateTimetable()
                    }
                }
            }

            //moves in weeks or cycles backward
            previousWeek.setOnClickListener {
                Log.i(TAG, "Previous pressed")

                if (mainVM.isPermanent.value != true) {

                    mainVM.selectedDate = mainVM.selectedDate.minusDays(7)

                } else {

                    mainVM.cycleIndex--
                    val week = currentVM.data.value
                    if (week != null && mainVM.cycleIndex < 0) {
                        mainVM.cycleIndex = week.cycles.size - 1
                    }
                }

                onSelectionChanged()
            }
            //moves in weeks or cycles forward
            nextWeek.setOnClickListener {
                Log.i(TAG, "Next pressed")

                if (mainVM.isPermanent.value != true) {
                    mainVM.selectedDate = mainVM.selectedDate.plusDays(7)

                } else {

                    mainVM.cycleIndex++
                    val week = currentVM.data.value
                    if (week != null && mainVM.cycleIndex > (week.cycles.size - 1)) {
                        mainVM.cycleIndex = 0
                    }
                }

                onSelectionChanged()
            }

            //changes to permanent or actual timetable
            permanentSwitch.setOnClickListener {
                if (mainVM.isPermanent.value == true) {
                    Log.i(TAG, "Switching to normal timetable")

                } else {
                    Log.i(TAG, "Switching to permanent timetable")
                }

                mainVM.isPermanent.value = mainVM.isPermanent.value != true
                onSelectionChanged()
            }

            /**Reloads timetable from server*/
            reload.setOnClickListener {
                Log.i(TAG, "Reload pressed")

                currentVM.loadData()
            }

            /**navigates back to today if user is somewhere in a future or a history*/
            home.setOnClickListener {
                Log.i(TAG, "Home pressed")

                mainVM.selectedDate = TimeTools.monday.toCzechDate()
                it.visibility = View.GONE
                onSelectionChanged()
            }

            /**shows date choose dialog*/
            calendar.setOnClickListener {

                val selectedDate = mainVM.selectedDate
                val listener: (Any, Int, Int, Int) -> Unit = { view, year, month, dayOfMonth ->

                    mainVM.selectedDate = LocalDate.of(year, month + 1, dayOfMonth).toMonday()
                    onSelectionChanged()
                }

                //custom time picker
                /**/com.wdullaer.materialdatetimepicker.date.DatePickerDialog.newInstance(
                listener,
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).apply {
                minDate = mainVM.rangeMin.toCalendar()
                maxDate = mainVM.rangeMax.toCalendar()
                isThemeDark = isDarkTheme(this@TimetableFragment.requireContext())
                vibrate(false)
                dismissOnPause(true) //listeners don't survive orientation changes
            }.show(childFragmentManager, "$TAG-Date picker")

                //system time picker
                /*android.app.DatePickerDialog(
                    requireContext(),
                    listener,
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth
                ).apply {
                    datePicker.apply {
                        minDate = rangeMin.toInstant().toEpochMilli()
                        maxDate = rangeMax.toInstant().toEpochMilli()
                    }
                }.show()*/
            }
        }
    }

    /**Starts data loading before views are layout*/
    private fun prepareDefaultViewModel() {
        val date =
            if (mainVM.isPermanent.value == true) TimeTools.PERMANENT else mainVM.selectedDate

        mainVM.getTimetableViewModel(date).data //triggers database query to obtain data
    }

    /**Updates timetable for new week / cycle*/
    private fun onSelectionChanged() {
        if (this::currentVM.isInitialized) {
            currentVM.data.removeObservers { lifecycle }
            currentVM.hasData.removeObservers { lifecycle }

            updatingJob?.cancel()
        }

        layoutForHeight = -1
        layoutForWeek = null
        layoutForCycle = null

        val date =
            if (mainVM.isPermanent.value == true) TimeTools.PERMANENT else mainVM.selectedDate

        currentVM = mainVM.getTimetableViewModel(date)
        binding.isProcessing = true
        binding.viewModel = currentVM

        currentVM.runOrRefresh(lifecycle) {
            updateTimetable()
        }
        currentVM.onDataUpdate(lifecycle) {
            //sets text like Next week, last updated 12:00 30.2.2020
            binding.lastUpdated.text = lastUpdatedText()
        }

        binding.apply {
            isPrevious = true
            isNext = true
            if (mainVM.isPermanent.value == false) {
                if (date.toMonday() <= mainVM.rangeMin.toMonday()) {
                    isPrevious = false
                }
                if (date.toMonday() >= mainVM.rangeMax.toMonday()) {
                    isNext = false
                }
            }
        }
    }

    private fun updateTimetable() = synchronized(this) {

        //loads timetable and row height
        if (!viewsLayout) return
        val height: Int = if (totalHeight != 0) totalHeight else return
        val week: Week = currentVM.data.value ?: return

        val cycle =
            if (mainVM.isPermanent.value == true) {
                if (week.cycles.size > 0)
                    week.cycles[mainVM.cycleIndex]
                else
                    null
            } else
                if (week.cycles.size > 0)
                    week.cycles[0]
                else
                    null

        if (height == layoutForHeight && week == layoutForWeek && cycle == layoutForCycle) return

        layoutForHeight = height
        layoutForWeek = week
        layoutForCycle = cycle

        if (updatingJob?.isActive == true) {
            updatingJob?.cancel()
        }

        updatingJob = lifecycleScope.launch(Dispatchers.Main) {

            if (currentVM.isEmpty.value == true) {

                //updates cycle name
                binding.edge.findViewById<TextView>(R.id.cycle).text = ""
            } else {

                binding.isProcessing = true

                yield()

                TimetableCreator.prepareTimetable(
                    binding.root,
                    height,
                    week.days.size,
                    week.trimFreeMorning().size
                )

                yield()

                //creates actual timetable
                TimetableCreator.createTimetable(
                    binding.root,
                    week,
                    cycle,
                    mainVM.user,
                    homeworkViewModel.homework.value
                )

                //updates cycle name
                binding.edge.findViewById<TextView>(R.id.cycle).text = cycle?.name ?: ""

                yield()
            }

            binding.isProcessing = false

        }
    }

    /**@return text saying what week is shown and when was this timetable downloaded*/
    private fun lastUpdatedText(): String {

        var toReturn = if (mainVM.isPermanent.value != true) {

            //how much has user moved
            val diff = ChronoUnit.DAYS.between(mainVM.selectedDate, TimeTools.monday.toCzechDate())
            val weeks = ((diff) / 7.0).toInt() // + 1 to prevent timezone issues

            //array used for saying how many week forward/backward user has gone
            val dataArray = App.getStringArray(R.array.week_variants)


            //show button whits takes user back to today
            binding.isHome = weeks !in -2..2

            //2-4 and 5+ because of czech inflection
            when (weeks) {
                in 5..Int.MAX_VALUE -> {
                    String.format(dataArray[4], abs(weeks), dataArray[6])
                }
                in 2..4 -> {
                    String.format(dataArray[3], abs(weeks), dataArray[6])
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
                    String.format(dataArray[3], abs(weeks), dataArray[5])
                }
//in Int.MIN_VALUE..-5
                else -> {
                    String.format(dataArray[4], abs(weeks), dataArray[5])
                }
            }
        } else
            getString(R.string.timetable_permanent)

//add when was timetable last updated
        val lastUpdated = currentVM.lastUpdated()
        lastUpdated?.let {
            toReturn += ", " + lastUpdated(App.context, it)
        }

        return toReturn
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (mainVM.isWebTimetableAvailable()) {
                yield()
                inflater.inflate(R.menu.timetable_old, menu)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_timetable_old) {

            val user = mainVM.user

            if (user != null) {
                mainVM.openWebTimetable(
                    requireContext(),
                    WebTimetableDate.ACTUAL,
                    WebTimetableType.CLASS,
                    user.classInfo.id
                )
            } else {
                mainVM.openWebTimetable(requireContext())
            }

            true
        } else
            super.onOptionsItemSelected(item)
    }
}
