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
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.databinding.HomeworkEntryBinding
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.attachment.AttachmentDialog
import java.time.Duration

/**sets up binding view with homework*/
class HmwEntryManager(
    val activity: AppCompatActivity,
    val binding: HomeworkEntryBinding,
    val homework: Homework
) {

    private val onPreDrawListener = ViewTreeObserver.OnPreDrawListener {
        onPreDraw()
        true
    }

    private var cycles = 0

    /**
     * cannot determinate if text view will have more lines when textIsSelectable = true
     * because of that it's turned off at the beginning and on the 2nd pass line number is
     * determined and text is made selectable
     * how many times was TextView preDrawn
     */
    private fun onPreDraw() {
        binding.apply {
            if (cycles < 2) { //works on second pass

                showMore.visibility =
                    if (isMoreLines()) {
                        cycles = 10
                        onPreDraw()

                        View.VISIBLE
                    } else
                        View.GONE
                cycles++
            } else {
                //stops listening for preDraw
                removeViewTreeObserver()
            }
        }
    }

    init {
        //observes for text preDraw
        binding.apply {
            template.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)

            //makes text selectable
            content.setTextIsSelectable(true)

            //makes links clickable
            content.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    /** Stops observing for text preDraw*/
    private fun removeViewTreeObserver() {
        binding.template.viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
    }

    //Replaces normal text strings with clickable links
    //replaced with autoLink - works with addressees without protocol, emails and phone numbers
    /*fun httpEnabledText(): CharSequence {

        //available protocols
        val protocols = arrayListOf("http://", "https://", "ftp://")
        //links are replaced with this string and then replaced with valid Html <a></a> tag
        val key = "\r\t\n\r"
        //characters that ends url
        val breakCharacters = " \n\r\t\'\""

        //text to be edited
        var text = homework.content

        for (protocol in protocols) {

            //list of links temporally replaced by keys
            val replacedLinks = ArrayList<String>()

            //runs until links are found
            while (true) {
                val index = text.indexOf(protocol)
                if (index < 0)
                    break

                //link characters are added here
                val builder = StringBuilder(68)
                builder.append(protocol)

                //locks for the end of the link of the end of the string
                for (i in index + protocol.length until text.length) {
                    val char = text[i]

                    if (breakCharacters.contains(char)) {
                        break
                    } else if (i == text.length - 1) {
                        builder.append(char)
                        break
                    } else {
                        builder.append(char)
                    }
                }

                //saves link text for future and replaces it in text with key
                val linkText = builder.toString()
                text = text.replaceFirst(linkText, key)
                replacedLinks.add(linkText)
            }

            //replaces keys with Html <a><a/> tag with original links
            for (linkText in replacedLinks) {
                val htmlLink = "<a href=\"$linkText\">$linkText</a>"
                text = text.replaceFirst(key, htmlLink)
            }
        }

        //parses string to spannable html with clickable links
        return Html.fromHtml(text)
    }*/

    //checks if string takes more than default number of lines in a TextView
    private fun isMoreLines(): Boolean {
        if (binding.showMore.text != activity.getString(R.string.homework_show_more))
            return true

        //layout may not has been created yet
        val layout: Layout = binding.template.layout ?: return false

        val lines = layout.lineCount
        if (lines > 0) {
            val ellipsisCount = layout.getEllipsisCount(lines - 1)
            if (ellipsisCount > 0) {
                return true
            }
        }
        return false
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
        val attachmentDialog = AttachmentDialog.newInstance(homework.attachments)
        attachmentDialog.show(activity, AttachmentDialog.FRAGMENT_TAG)
    }

    /**@return the text with the number of attachment of the homework*/
    fun getAttachmentText(): String {
        val length = homework.attachments.size
        return activity.resources.getQuantityString(R.plurals.attachment, length, length)
    }

    /**@return how many pretences of time between start day 00:00 and end day 23:59 has left*/
    fun getProgress(): Int {
        val total = Duration.between(homework.dateStartDate, homework.dateEndDate.plusDays(1))
        val gone = Duration.between(homework.dateStartDate, TimeTools.now)

        return (gone.seconds.toDouble() / total.seconds.toDouble() * 100.0).toInt()
    }

    /**formats start date*/
    fun formattedStart(): String {
        return TimeTools.format(homework.dateStartDate, "d.M.")
    }

    /**formats end date*/
    fun formattedEnd(): String {
        return TimeTools.format(homework.dateEndDate, "d.M.")
    }

    /**@return how many days are left until homework end*/
    fun daysLeftText(): String {
        val diff = Duration.between(TimeTools.today, TimeTools.toMidnight(homework.dateEndDate))
        return "${diff.toDays()} ${activity.resources.getQuantityString(
            R.plurals.last_updated_days,
            diff.toDays().toInt()
        )}"
    }
}