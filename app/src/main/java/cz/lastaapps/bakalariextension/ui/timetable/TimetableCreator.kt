package cz.lastaapps.bakalariextension.ui.timetable

import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.TTData
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZoneId

class TimetableCreator {
    companion object {
        fun createTimetable(root: View, week: Week, cycle: TTData.Cycle?) {

            val daysTable = root.findViewById<LinearLayout>(R.id.table_days)
            val table = root.findViewById<TableLayout>(R.id.table)

            val context = root.context
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            /**Height of one table row*/
            val height = root.findViewById<ViewGroup>(R.id.table_box).measuredHeight / 6


            //changes day's days and shortcuts in first day column
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

                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                view.layoutParams = LinearLayout.LayoutParams(view.measuredWidth, height)

                val dayTV = view.findViewById<TextView>(R.id.day)
                val dateTV = view.findViewById<TextView>(R.id.date)

                dayTV.text = App.getString(dayShortcutsArray[i])
                if (day.date != "")
                    dateTV.text = TimeTools.format(day.toDate(), "d.M.", ZoneId.systemDefault())
                else
                //For permanent timetable
                    dateTV.text = ""
            }


            //resets timetable data
            table.removeAllViews()


            //init cycle name, cycle in week is null during empty weeks
            val edge = daysTable.findViewById<ViewGroup>(R.id.edge)
            edge.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            edge.layoutParams = LinearLayout.LayoutParams(edge.measuredWidth, height)

            //non valid hour would result in empty columns (like zero lessons)
            val validHours = week.getNotEmptyHours()
            if (validHours.isEmpty() || cycle == null) {

                edge.findViewById<TextView>(R.id.cycle).text = ""

                root.findViewById<TextView>(R.id.empty_timetable)
                    .visibility = View.VISIBLE

                return
            } else {

                //init cycle name
                edge.findViewById<TextView>(R.id.cycle).text =
                    cycle.name

                root.findViewById<TextView>(R.id.empty_timetable)
                    .visibility = View.GONE
            }


            //first row with lesson start/end times and captions
            val numberRow = TableRow(context)
            table.addView(numberRow, 0)
            for (i in 0 until week.hours.size) {
                val hour = week.hours[i]

                //deleting free columns
                if (!validHours.contains(hour))
                    continue

                val viewGroup = inflater.inflate(R.layout.timetable_number, null)

                viewGroup.findViewById<TextView>(R.id.caption).text = hour.caption
                viewGroup.findViewById<TextView>(R.id.begin).text = hour.begin
                viewGroup.findViewById<TextView>(R.id.end).text = hour.end

                val params = TableRow.LayoutParams()
                params.height = height
                viewGroup.layoutParams = params
                numberRow.addView(viewGroup)

            }


            //creating actual timetable
            for (day in week.days) {
                val row = TableRow(context)
                table.addView(row)

                for (i in 0 until week.hours.size) {
                    var viewGroup: View

                    val hour = week.hours[i]

                    //deleting free columns
                    if (!validHours.contains(hour))
                        continue

                    val lesson = day.getLesson(hour, cycle)

                    if (lesson != null) {

                        if (!lesson.isAbsence()) {

                            if (lesson.isNormal()) {
                                viewGroup = inflater.inflate(R.layout.timetable_lesson, null)

                                viewGroup.findViewById<TextView>(R.id.subject).text =
                                    week.subjects.getById(lesson.subjectId)?.shortcut
                                viewGroup.findViewById<TextView>(R.id.room).text =
                                    week.rooms.getById(lesson.roomId)?.shortcut

                                val teacher = week.teachers.getById(lesson.teacherId)
                                viewGroup.findViewById<TextView>(R.id.teacher).text =
                                    if (teacher != null)
                                        week.teachers.getById(lesson.teacherId)?.shortcut
                                    else
                                    //for teachers
                                        week.groups.getById(lesson.groupIds)?.shortcut

                            } else {
                                viewGroup = inflater.inflate(R.layout.timetable_free, null)
                            }

                            if (lesson.change != null) {
                                viewGroup.setBackgroundColor(App.getColor(R.color.timetable_change))
                            }

                        } else {
                            viewGroup = inflater.inflate(R.layout.timetable_absence, null)
                            viewGroup.setBackgroundColor(App.getColor(R.color.timetable_absence))

                            viewGroup.findViewById<TextView>(R.id.absence).text =
                                lesson.change!!.typeShortcut
                        }

                        showLessonInfo(viewGroup, week, lesson)

                    } else {
                        viewGroup = inflater.inflate(R.layout.timetable_free, null)
                    }

                    val now = TimeTools.calToSeconds(
                        TimeTools.now.toLocalTime()
                    )
                    val begin = TimeTools.calToSeconds(
                        TimeTools.parseTime(hour.begin, TimeTools.TIME_FORMAT, TimeTools.CET)
                    )
                    val end = TimeTools.calToSeconds(
                        TimeTools.parseTime(hour.end, TimeTools.TIME_FORMAT, TimeTools.CET)
                    )

                    if (day.isNormal(hour)) {
                        val dayDate = day.toDate()
                        if (TimeTools.toDate(
                                dayDate,
                                dayDate.zone
                            ) == TimeTools.toDate(TimeTools.now, dayDate.zone)
                            && now in begin..end
                        ) {
                            viewGroup.setBackgroundColor(App.getColor(R.color.timetable_current))
                        }
                    }

                    val params = TableRow.LayoutParams()
                    params.height = height
                    viewGroup.layoutParams = params

                    row.gravity = Gravity.CENTER_VERTICAL
                    row.addView(viewGroup)
                }
            }
        }

        private fun showLessonInfo(view: View, week: Week, lesson: Lesson) {
            view.setOnClickListener {

                val inflater =
                    it.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val root = inflater.inflate(R.layout.timetable_info, null)
                val table = root.findViewById<TableLayout>(R.id.table)

                val addInfoRow = { table: TableLayout, field: String?, fieldName: String ->
                    if (field != "" && field != null && field != "null") {

                        val inflater =
                            table.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                        val row = inflater.inflate(R.layout.timetable_info_row, null)
                        row.findViewById<TextView>(R.id.name).text = fieldName
                        row.findViewById<TextView>(R.id.value).text = field

                        table.addView(row)
                    }
                }

                addInfoRow(table, lesson.change?.typeName, "${App.getString(R.string.info_name)}:")
                addInfoRow(
                    table,
                    week.subjects.getById(lesson.subjectId)?.name,
                    "${App.getString(R.string.info_subject)}:"
                )
                addInfoRow(
                    table,
                    week.teachers.getById(lesson.teacherId)?.name,
                    "${App.getString(R.string.info_teacher)}:"
                )
                addInfoRow(
                    table,
                    week.rooms.getById(lesson.roomId)?.name,
                    "${App.getString(R.string.info_room)}:"
                )
                addInfoRow(table, lesson.theme, "${App.getString(R.string.info_theme)}:")
                addInfoRow(table, {
                    val builder = StringBuffer()
                    for (group in lesson.groupIds) {
                        builder.append(week.groups.getById(group)?.shortcut)
                        builder.append(", ")
                    }
                    builder.toString().substring(0, builder.length - 2)
                }.invoke(), "${App.getString(R.string.info_group)}:")
                addInfoRow(
                    table,
                    lesson.change?.description,
                    "${App.getString(R.string.info_change)}:"
                )
                addInfoRow(table, lesson.change?.time, "${App.getString(R.string.info_time)}:")

                //TODO Homework


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

}