package cz.lastaapps.bakalariextension.ui.timetable

import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.api.timetable.Timetable
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import kotlin.math.abs

class TimetableFragment : Fragment() {

    companion object {
        private val TAG = TimetableFragment::class.java.simpleName

        private const val CALENDAR_KEY = "calendar"
    }

    lateinit var root: View
    lateinit var calendar: ZonedDateTime
    var height: Int = 0
    var isPermanent = false
    lateinit var lastCalendar: ZonedDateTime
    var cycleIndex = 0
    var week: Week? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        calendar = TimeTools.toMonday(
            TimeTools.cal
        )
        if (savedInstanceState?.getSerializable(CALENDAR_KEY) != null)
            calendar = savedInstanceState.getSerializable(CALENDAR_KEY) as ZonedDateTime
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_timetable, container, false)

        val tableBox = root.findViewById<ViewGroup>(R.id.table_box)
        tableBox.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                height = tableBox.measuredHeight / 6
                //val width: Int = edge.measuredWidth
                if (height != 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        tableBox.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        tableBox.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
            }
        })

        root.findViewById<ImageButton>(R.id.previous_week).setOnClickListener {
            if (!isPermanent) {
                calendar = TimeTools.previousWeek(calendar)
                updateTimetable(calendar)
            } else {
                cycleIndex--
                val week = week
                if (week != null && cycleIndex < 0 ) {
                    cycleIndex = week.cycles.size - 1
                }
                updateTimetable(TimeTools.PERMANENT)
            }
        }
        root.findViewById<ImageButton>(R.id.next_week).setOnClickListener {
            if (!isPermanent) {
                calendar = TimeTools.nextWeek(calendar)
                updateTimetable(calendar)
            } else {
                cycleIndex++
                val week = week
                if (week != null && cycleIndex > (week.cycles.size - 1)) {
                    cycleIndex = 0
                }
                updateTimetable(TimeTools.PERMANENT)
            }
        }

        root.findViewById<ImageButton>(R.id.permanent_switch).setOnClickListener {
            if (isPermanent) {
                cycleIndex = 0
                updateTimetable(calendar)
                (it as ImageButton).setImageDrawable(
                    root.context.resources.getDrawable(R.drawable.permanent)
                )

            } else {
                updateTimetable(TimeTools.PERMANENT)
                (it as ImageButton).setImageDrawable(
                    root.context.resources.getDrawable(R.drawable.actual)
                )
            }
            isPermanent = !isPermanent
        }
        root.findViewById<ImageButton>(R.id.reload).setOnClickListener {
            updateTimetable(lastCalendar, true)
        }

        updateTimetable(calendar)

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(CALENDAR_KEY, calendar)

        return super.onSaveInstanceState(outState)
    }

    private fun updateTimetable(cal: ZonedDateTime, forceReload: Boolean = false) {
        LoadTask().execute(cal, forceReload)
    }

    inner class LoadTask : AsyncTask<Any, Any, Week?>() {

        override fun onPreExecute() {
            val progressBar = root.findViewById<ProgressBar>(R.id.progress_bar)
            val errorMessage = root.findViewById<TextView>(R.id.error_message)
            val table = root.findViewById<View>(R.id.table_box)
            val bottomBox = root.findViewById<ViewGroup>(R.id.bottom_box)

            progressBar.visibility = View.VISIBLE
            errorMessage.visibility = View.GONE
            table.visibility = View.INVISIBLE
            bottomBox.visibility = View.INVISIBLE
            bottomBox.isEnabled = false
        }

        override fun doInBackground(vararg params: Any?): Week? {
            val cal = params[0] as ZonedDateTime
            val forceReload = params[1] as Boolean

            lastCalendar = cal

            week = Timetable.loadTimetable(cal, forceReload)

            while (height == 0)
                Thread.sleep(1)

            return week
        }

        override fun onPostExecute(week: Week?) {

            val progressBar = root.findViewById<ProgressBar>(R.id.progress_bar)
            val errorMessage = root.findViewById<TextView>(R.id.error_message)
            val table = root.findViewById<View>(R.id.table_box)
            val bottomBox = root.findViewById<ViewGroup>(R.id.bottom_box)
            bottomBox.isEnabled = true
            val lastUpdated = root.findViewById<TextView>(R.id.last_updated)

            progressBar.visibility = View.GONE

            if (week == null) {
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = getString(R.string.error_no_timetable_no_internet)
                lastUpdated.text = ""
            } else {
                table.visibility = View.VISIBLE

                lastUpdated.text = lastUpdatedText()

                val cycle = if (week.cycles.size > 0)
                    week.cycles[cycleIndex]
                else
                    null

                TimetableCreator.createTimetable(root, week, cycle)
            }
            bottomBox.visibility = View.VISIBLE
        }

        private fun lastUpdatedText(): String {

            var toReturn =
                if (lastCalendar != TimeTools.PERMANENT) {
                    var diff = (TimeTools.toMonday(
                        TimeTools.cal
                    ).toEpochSecond() -
                            lastCalendar.toEpochSecond()).toInt()

                    diff /= 60 * 60 * 24 * 7

                    val dataArray = App.getStringArray(R.array.week_forms)

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
                            TTStorage.lastUpdated(lastCalendar)!!,
                            "HH:mm d.M.", ZoneId.systemDefault()
                        )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return toReturn
        }
    }


}
