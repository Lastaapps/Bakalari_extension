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

package cz.lastaapps.bakalariextension.ui.homework

import android.text.Layout
import android.text.TextUtils
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.MobileNavigationDirections
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.EntryHomeworkBinding
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toMidnight
import java.time.Duration

/**sets up binding view with homework*/
class HmwEntryManager(
    val activity: AppCompatActivity,
    val binding: EntryHomeworkBinding,
    val homework: Homework
) {

    companion object {
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
        binding.apply {
            template.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)

            showMore.visibility = View.GONE
            showMore.text = activity.getString(R.string.homework_show_more)
        }
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
        val res = activity.resources
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

    /**Manages on attachment clicked*/
    fun onAttachment() {
        val attachmentDialog =
            MobileNavigationDirections.actionAttachment(homework.attachments.toTypedArray())
        activity.findNavController(R.id.nav_host_fragment).navigate(attachmentDialog)
    }

    /**@return the text with the number of attachment of the homework*/
    fun getAttachmentText(): String {
        val length = homework.attachments.size
        return activity.resources.getQuantityString(R.plurals.attachment, length, length)
    }

    /**@return how many pretences of time between start day 00:00 and end day 23:59 has left*/
    fun getProgress(): Int {
        val total = Duration.between(homework.dateStart, homework.dateEnd.plusDays(1))
        val gone = Duration.between(homework.dateStart, TimeTools.now)

        return (gone.seconds.toDouble() / total.seconds.toDouble() * 100.0).toInt()
    }

    /**formats start date*/
    fun formattedStart(): String {
        return TimeTools.format(homework.dateStart, "d.M.")
    }

    /**formats end date*/
    fun formattedEnd(): String {
        return TimeTools.format(homework.dateEnd, "d.M.")
    }

    /**@return how many days are left until homework end*/
    fun daysLeftText(): String {
        val diff = Duration.between(TimeTools.today, homework.dateEnd.toMidnight())
        return "${diff.toDays()} ${activity.resources.getQuantityString(
            R.plurals.last_updated_days,
            diff.toDays().toInt()
        )}"
    }
}