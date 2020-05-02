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

package cz.lastaapps.bakalariextension.ui.timetable

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.*
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlin.math.max

/**Provides methods for settings up lesson cells*/
class CellSetup {

    companion object {

        /**updates text in view inflated from R.layout.timetable_lesson
         * @param highlight if should actual lesson highlighted*/
        fun setUpCell(
            view: View, week: Week, day: Day, hour: Hour, cycle: Cycle?,
            highlight: Boolean = true
        ) {
            val map = getStrings(week, day, hour, cycle)

            view.findViewById<TextView>(R.id.subject).text = map[R.id.subject]
            view.findViewById<TextView>(R.id.room).text = map[R.id.room]
            view.findViewById<TextView>(R.id.teacher).text = map[R.id.teacher]

            view.setBackgroundColor(getBackgroundColor(week, day, hour, cycle, highlight))

            if (isHomeworkWarningVisible(week, day, hour, cycle))
                view.findViewById<ImageView>(R.id.homework_warning).visibility = View.VISIBLE
        }

        /**@return Map<resource id of the TextView, text>*/
        fun getStrings(
            week: Week, day: Day, hour: Hour, cycle: Cycle?
        ): HashMap<Int, String> {
            val map = HashMap<Int, String>()

            val lesson = day.getLesson(hour, cycle)

            //for normal lesson
            if (day.isNormal(hour, cycle)) {
                lesson?.let {
                    map[R.id.subject] = week.subjects.getById(lesson.subjectId)?.shortcut ?: ""
                    map[R.id.room] = week.rooms.getById(lesson.roomId)?.shortcut ?: ""

                    val teacher = week.teachers.getById(lesson.teacherId)
                    map[R.id.teacher] = (
                            if (teacher != null)
                                week.teachers.getById(lesson.teacherId)?.shortcut
                            else
                            //for teachers
                                week.groups.getById(lesson.groupIds)?.shortcut
                            ) ?: ""
                }
            }
            //for absence
            if (day.isAbsence(hour, cycle)) {
                lesson?.let {
                    map[R.id.subject] =
                        lesson.change!!.typeShortcut
                    map[R.id.room] = ""
                    map[R.id.teacher] = ""
                }
            }
            //for free lesson - empty
            if (day.isFree(hour, cycle)) {
                map[R.id.subject] = ""
                map[R.id.room] = ""
                map[R.id.teacher] = ""
            }

            return map
        }

        /**@param highlight if cell can be highlighted as actual
         * @return background color for the cell*/
        fun getBackgroundColor(
            week: Week, day: Day, hour: Hour, cycle: Cycle?,
            highlight: Boolean
        ): Int {
            val lesson = day.getLesson(hour, cycle)

            var color = App.getColor(android.R.color.transparent)

            if (lesson != null) {
                //lesson changed
                if (lesson.isChanged())
                    color = App.getColor(R.color.timetable_change)
                //lesson absence
                if (lesson.isAbsence())
                    color = App.getColor(R.color.timetable_absence)
            }//else no lesson there

            if (highlight) {
                //highlight current lesson
                if (day.isNormal(hour, cycle)) {
                    val now = TimeTools.timeToSeconds(
                        TimeTools.now.toLocalTime()
                    )
                    val begin = TimeTools.timeToSeconds(
                        TimeTools.parseTime(hour.begin, TimeTools.TIME_FORMAT, TimeTools.CET)
                    )
                    val end = TimeTools.timeToSeconds(
                        TimeTools.parseTime(hour.end, TimeTools.TIME_FORMAT, TimeTools.CET)
                    )
                    val dayDate = day.toDate()

                    if (TimeTools.toDate(dayDate, dayDate.zone)
                        == TimeTools.toDate(TimeTools.now, dayDate.zone)
                        && now in begin..end
                    ) {
                        color = App.getColor(R.color.timetable_current)
                    }
                }
            }

            return color
        }

        fun isHomeworkWarningVisible(
            week: Week, day: Day, hour: Hour, cycle: Cycle?
        ): Boolean {
            if (!week.isPermanent()) {
                val lesson = day.getLesson(hour, cycle)
                if (lesson != null) {
                    return lesson.homeworkIds.isNotEmpty()
                }
            }
            return false
        }
    }

    /**Shows lesson info on click*/
    class ShowLessonInfo(
        val week: Week, val day: Day, val hour: Hour, val cycle: Cycle?
    ) : View.OnClickListener {

        override fun onClick(view: View) {

            val lesson = day.getLesson(hour, cycle) ?: return

            val inflater =
                view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val root = inflater.inflate(R.layout.timetable_lesson_info, null)
            val table = root.findViewById<TableLayout>(R.id.table)

            //function to add entry into dialog
            val addInfoRow = { table: TableLayout, field: String?, fieldName: String ->
                if (field != "" && field != null && field != "null") {

                    val row = inflater.inflate(
                        R.layout.timetable_lesson_info_row, table, false
                    )
                    //puts entry inside
                    row.findViewById<TextView>(R.id.name).text = fieldName
                    row.findViewById<TextView>(R.id.value).text = field

                    table.addView(row)
                }
            }

            //adds all available info
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
                builder.toString().substring(0, max(0, builder.length - 2))
            }.invoke(), "${App.getString(R.string.info_group)}:")
            addInfoRow(
                table,
                lesson.change?.description,
                "${App.getString(R.string.info_change)}:"
            )
            addInfoRow(
                table, when (lesson.change?.changeType) {
                    Change.ADDED -> App.getString(R.string.change_added)
                    Change.REMOVED -> App.getString(R.string.change_removed)
                    Change.CANCELED -> App.getString(R.string.change_canceled)
                    else -> ""
                }, "${App.getString(R.string.info_change_type)}:"
            )
            addInfoRow(table, lesson.change?.time, "${App.getString(R.string.info_time)}:")

            //TODO Homework

            //shows dialog with this info
            AlertDialog.Builder(ContextThemeWrapper(view.context, R.style.Timetable_Info))
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