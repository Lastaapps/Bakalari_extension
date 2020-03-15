package cz.lastaapps.bakalariextension.ui.timetable

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.*
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.api.timetable.TTTools
import cz.lastaapps.bakalariextension.api.timetable.Timetable
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import java.util.*
import kotlin.math.abs

class TimetableFragment : Fragment() {

    companion object {
        private val TAG = TimetableFragment::class.java.simpleName

        private const val CALENDAR_KEY = "calendar"
    }

    lateinit var root: View
    lateinit var calendar: Calendar
    var height: Int = 0
    var isPermanent = false
    lateinit var lastCalendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        calendar = TTTools.toMonday(TTTools.cal)
        if (savedInstanceState?.getSerializable(CALENDAR_KEY) != null)
            calendar = savedInstanceState.getSerializable(CALENDAR_KEY) as Calendar
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
            calendar = TTTools.previousWeek(calendar)
            updateTimetable(calendar)
        }
        root.findViewById<ImageButton>(R.id.next_week).setOnClickListener {
            calendar = TTTools.nextWeek(calendar)
            updateTimetable(calendar)
        }
        root.findViewById<ImageButton>(R.id.permanent_switch).setOnClickListener {
            if (isPermanent) {
                updateTimetable(calendar)
                (it as ImageButton).setImageDrawable(
                    root.context.resources.getDrawable(R.drawable.permanent)
                )
                root.findViewById<ImageButton>(R.id.next_week).visibility = View.VISIBLE
                root.findViewById<ImageButton>(R.id.previous_week).visibility = View.VISIBLE

            } else {
                updateTimetable(TTTools.PERMANENT)
                (it as ImageButton).setImageDrawable(
                    root.context.resources.getDrawable(R.drawable.actual)
                )
                root.findViewById<ImageButton>(R.id.next_week).visibility = View.GONE
                root.findViewById<ImageButton>(R.id.previous_week).visibility = View.GONE
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

    private fun updateTimetable(cal: Calendar, forceReload: Boolean = false) {
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
        }

        override fun doInBackground(vararg params: Any?): Week? {
            val cal = params[0] as Calendar
            val forceReload = params[1] as Boolean

            lastCalendar = cal

            val week = Timetable.loadTimetable(cal, forceReload)

            while (height == 0)
                Thread.sleep(1)

            return week
        }

        override fun onPostExecute(week: Week?) {

            val progressBar = root.findViewById<ProgressBar>(R.id.progress_bar)
            val errorMessage = root.findViewById<TextView>(R.id.error_message)
            val table = root.findViewById<View>(R.id.table_box)
            val bottomBox = root.findViewById<ViewGroup>(R.id.bottom_box)
            val lastUpdated = root.findViewById<TextView>(R.id.last_updated)

            progressBar.visibility = View.GONE

            if (week == null) {
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = getString(R.string.error_no_timetable_no_internet)
                lastUpdated.text = ""
            } else {
                table.visibility = View.VISIBLE

                lastUpdated.text = lastUpdatedText()

                createTimetable(week)
            }
            bottomBox.visibility = View.VISIBLE
        }

        private fun lastUpdatedText(): String {

            var toReturn =
                if (lastCalendar != TTTools.PERMANENT) {
                    var diff = (TTTools.toMonday(TTTools.cal).time.time / 1000).toInt() -
                            (lastCalendar.time.time / 1000).toInt()

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

            toReturn += ", " + getString(R.string.last_updated) + " " +
                    TTTools.format(
                        TTStorage.lastUpdated(lastCalendar)!!,
                        TimeZone.getDefault().displayName, "HH:mm d.M."
                    )

            return toReturn
        }
    }

    private fun createTimetable(week: Week) {

        val daysTable = root.findViewById<LinearLayout>(R.id.table_days)
        val table = root.findViewById<TableLayout>(R.id.table)

        val context = root.context

        val edge = daysTable.findViewById<ViewGroup>(R.id.edge)
        edge.findViewById<TextView>(R.id.cycle).text =
            week.cycleName

        val dayArray =
            arrayOf(R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday)
        val dayShortcutsArray = arrayOf(
            R.string.monday_shortut,
            R.string.tuesday_shortut,
            R.string.wednesday_shortut,
            R.string.thursday_shortut,
            R.string.friday_shortut
        )
        for (i in 0 until 5) {
            val day = week.days[i]
            val view = daysTable.findViewById<ViewGroup>(dayArray[i])

            val dayTV = view.findViewById<TextView>(R.id.day)
            val dateTV = view.findViewById<TextView>(R.id.date)

            dayTV.text = getString(dayShortcutsArray[i])
            if (day.date != "")
                dateTV.text = TTTools.format(day.toCal(), TTTools.CET, "d.M.")
            else
                dateTV.text = ""
        }

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        table.removeAllViews()

        val numberRow = TableRow(context)
        table.addView(numberRow, 0)
        for (i in 0 until week.patterns.size) {
            val pattern = week.patterns[i]
            val viewGroup = inflater.inflate(R.layout.timetable_number, null)

            viewGroup.findViewById<TextView>(R.id.caption).text = pattern.caption
            viewGroup.findViewById<TextView>(R.id.begin).text = pattern.begin
            viewGroup.findViewById<TextView>(R.id.end).text = pattern.end

            val params = TableRow.LayoutParams()
            params.height = height
            viewGroup.layoutParams = params
            numberRow.addView(viewGroup)
        }

        for (day in week.days) {
            val row = TableRow(context)
            table.addView(row)
            for (lesson in day.lessons) {
                var viewGroup: View
                when {
                    lesson.isNormal() -> {
                        viewGroup = inflater.inflate(R.layout.timetable_lesson, null)

                        viewGroup.findViewById<TextView>(R.id.subject).text = lesson.subjectShortcut
                        viewGroup.findViewById<TextView>(R.id.room).text = lesson.roomShortcut
                        viewGroup.findViewById<TextView>(R.id.teacher).text =
                            if (lesson.teacherShortcut == "")
                                lesson.teacherShortcut
                            else
                                //for teachers
                                lesson.groupShortcut

                    }

                    lesson.isAbsence() -> {
                        viewGroup = inflater.inflate(R.layout.timetable_absence, null)

                        viewGroup.findViewById<TextView>(R.id.absence).text = lesson.shortcut

                        viewGroup.setBackgroundColor(App.getColor(R.color.timetable_absence))
                    }

                    else -> {
                        viewGroup = inflater.inflate(R.layout.timetable_free, null)
                    }
                }

                if (lesson.change != "") {
                    viewGroup.setBackgroundColor(App.getColor(R.color.timetable_change))
                }

                val pattern = week.getPatternForLesson(lesson)
                if (pattern != null) {
                    val now = TTTools.calToSeconds(TTTools.now)
                    val begin = TTTools.calToSeconds(TTTools.parseTime(pattern.begin, TTTools.CET))
                    val end = TTTools.calToSeconds(TTTools.parseTime(pattern.end, TTTools.CET))

                    if (day.toCal().time == TTTools.cal.time
                        && now in begin..end
                    ) {
                        viewGroup.setBackgroundColor(App.getColor(R.color.timetable_current))
                    }
                }

                showLessonInfo(viewGroup, lesson)

                val params = TableRow.LayoutParams()
                params.height = height
                viewGroup.layoutParams = params

                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(viewGroup)
            }
        }
    }

    private fun showLessonInfo(view: View, lesson: Lesson) {
        view.setOnClickListener {

            val inflater =
                it.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val root = inflater.inflate(R.layout.timetable_info, null)
            val table = root.findViewById<TableLayout>(R.id.table)

            val addInfoRow = { table: TableLayout, field: String, fieldName: String ->
                if (field != "") {

                    val inflater =
                        table.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                    val row = inflater.inflate(R.layout.timetable_info_row, null)
                    row.findViewById<TextView>(R.id.name).text = fieldName
                    row.findViewById<TextView>(R.id.value).text = field

                    table.addView(row)
                }
            }

            addInfoRow(table, lesson.name, "${getString(R.string.info_name)}:")
            addInfoRow(table, lesson.subject, "${getString(R.string.info_subject)}:")
            addInfoRow(table, lesson.teacher, "${getString(R.string.info_teacher)}:")
            addInfoRow(table, lesson.room, "${getString(R.string.info_room)}:")
            addInfoRow(table, lesson.theme, "${getString(R.string.info_theme)}:")
            addInfoRow(table, lesson.absence, "${getString(R.string.info_absence)}:")
            addInfoRow(table, lesson.group, "${getString(R.string.info_group)}:")
            addInfoRow(table, lesson.change, "${getString(R.string.info_change)}:")
            addInfoRow(table, lesson.notice, "${getString(R.string.info_notice)}:")


            AlertDialog.Builder(ContextThemeWrapper(it.context, R.style.Timetable_Info))
                .setCancelable(true)
                .setPositiveButton(R.string.close) { dialog: DialogInterface?, _: Int ->
                    dialog?.dismiss()
                }
                .setView(root)
                .create()
                .show()
        }
    }

}
