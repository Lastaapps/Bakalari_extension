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

package cz.lastaapps.bakalari.app.ui.user.events

import android.content.Context
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewTreeObserver
import androidx.navigation.findNavController
import cz.lastaapps.bakalari.api.core.events.holders.Event
import cz.lastaapps.bakalari.app.NavGraphUserDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.EntryEventBinding
import cz.lastaapps.bakalari.tools.TimeTools

/**Actions for the entry showing the event info*/
class EventEntryManager(val binding: EntryEventBinding, val event: Event) {
    val context: Context = binding.root.context

    companion object {
        //states of the multiline section
        private const val LINES_UNKNOWN = -1
        private const val LINES_NORMAL = 0
        private const val LINES_MORE = 1
    }

    private val onPreDrawListener = ViewTreeObserver.OnPreDrawListener {
        onPreDraw()
    }

    private var cycles = 0

    /**
     * cannot determinate if text view will have more lines when textIsSelectable = true
     * because of that it's turned off at the beginning and on the 2nd pass line number is
     * determined and text is made selectable
     * how many times was TextView preDrawn
     */
    private fun onPreDraw(): Boolean {
        //the opposite of the if layout changed, so it shouldn't be drawn yet
        var viewsUnchanged = true

        binding.apply {
            if (cycles < 2) { //works on second pass

                cycles++

                when (isMoreLines()) {
                    LINES_UNKNOWN -> {
                        viewsUnchanged = false
                    }
                    LINES_NORMAL -> {
                        viewsUnchanged = cycles != 1
                    }
                    LINES_MORE -> {
                        showMore.visibility = View.VISIBLE
                        viewsUnchanged = false

                        cycles = Int.MAX_VALUE
                        onPreDraw()
                    }
                }

            } else {
                //stops listening for preDraw
                removeViewTreeObserver()
            }
        }

        return viewsUnchanged
    }

    init {
        //observes for text preDraw
        binding.template.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)

        binding.showMore.visibility = View.GONE
        binding.showMore.text = context.getString(R.string.homework_show_more)
    }

    /** Stops observing for text preDraw*/
    private fun removeViewTreeObserver() {
        binding.template.viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
    }

    //checks if string takes more than default number of lines in a TextView
    private fun isMoreLines(): Int {

        //layout may not has been created yet
        val layout: Layout = binding.template.layout ?: return LINES_UNKNOWN

        val lines = layout.lineCount
        if (lines > 0) {
            val ellipsisCount = layout.getEllipsisCount(lines - 1)
            if (ellipsisCount > 0) {
                return LINES_MORE
            }
        }
        return LINES_NORMAL
    }

    /**Shows/hides all the lines of the homework content*/
    fun onShowMoreClick() {
        val res = context.resources
        binding.apply {
            if (showMore.text == res.getString(R.string.homework_show_more)) {
                //shows all lines
                showMore.text = res.getString(R.string.homework_show_less)

                content.ellipsize = null
                content.maxLines = Integer.MAX_VALUE
            } else {
                //shows only some lines
                showMore.text = res.getString(R.string.homework_show_more)

                //removes text selection and scrolls to the top
                content.setTextIsSelectable(false)
                content.setTextIsSelectable(true)

                content.ellipsize = TextUtils.TruncateAt.END
                content.maxLines = res.getInteger(R.integer.homework_visible_lines)
            }
        }

    }

    fun classesString(): CharSequence {
        val list = event.classes
        val builder = StringBuilder(16)

        builder.append(context.getString(R.string.events_participants_classes))

        builder.append(": ")

        for (i in 0 until list.size) {
            var name = list[i].name
            if (name == "") name = list[i].shortcut

            builder.append(name)

            if (i < list.lastIndex) {
                builder.append(", ")
            }
        }

        return builder.toString()
    }

    fun teachersString(): CharSequence {
        val list = event.teachers
        val builder = SpannableStringBuilder()

        builder.append(context.getString(R.string.events_participants_teachers))

        builder.append(": ")

        for (i in 0 until list.size) {
            val teacher = list[i]
            var name = teacher.name
            if (name == "") name = teacher.shortcut

            val lastIndex = builder.lastIndex
            val teacherNameEnd = lastIndex + name.length + 1

            val span = object : ClickableSpan() {
                override fun onClick(v: View) {
                    v.findNavController().navigate(
                        NavGraphUserDirections.actionTeacherInfo(teacher.id)
                    )
                }
            }

            builder.append(name)

            builder.setSpan(span, lastIndex, teacherNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (i < list.lastIndex) {
                builder.append(", ")
            }
        }

        return builder
    }

    fun roomsString(): CharSequence {
        val list = event.rooms
        val builder = StringBuilder(16)

        builder.append(context.getString(R.string.events_participants_rooms))

        builder.append(": ")

        for (i in 0 until list.size) {
            var name = list[i].name
            if (name == "") name = list[i].shortcut

            builder.append(name)

            if (i < list.lastIndex) {
                builder.append(", ")
            }
        }

        return builder
    }

    fun studentsString(): CharSequence {
        val list = event.students
        val builder = StringBuilder(16)

        builder.append(context.getString(R.string.events_participants_students))

        builder.append(": ")

        for (i in 0 until list.size) {
            var name = list[i].name
            if (name == "") name = list[i].shortcut

            builder.append(name)

            if (i < list.lastIndex) {
                builder.append(", ")
            }
        }

        return builder
    }

    /**@return appropriate time representation depending on the period length*/
    fun dateText(): String = event.run {
        when {
            isPartOfDay -> {
                val date = TimeTools.format(eventStart, "d.M.")
                val timeStart = TimeTools.format(eventStart, "H:mm")
                val timeEnd = TimeTools.format(eventEnd, "H:mm")

                String.format("%s %s - %s", date, timeStart, timeEnd)
            }
            isOneDay -> {
                val date = TimeTools.format(eventStart, "d.M.")
                val wholeDay = context.getString(R.string.events_whole_day)

                String.format("%s - %s", date, wholeDay)
            }
            isMoreDays -> {
                val dateStart = TimeTools.format(eventStart, "d.M.")
                val dateEnd = TimeTools.format(eventEnd, "d.M.")

                String.format("%s - %s", dateStart, dateEnd)
            }
            else -> {
                ""
            }
        }
    }

}