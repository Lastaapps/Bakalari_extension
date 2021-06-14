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

package cz.lastaapps.bakalari.tools.ui.customdialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import cz.lastaapps.bakalari.tools.ui.BasicRecyclerAdapter
import cz.lastaapps.bakalari.tools.ui.R

/**Holds the method for dialog_... layout files*/

val TITLE = R.id.title
val IMAGE = R.id.image
val MESSAGE = R.id.message
val LIST = R.id.list
val PROGRESS_BAR = R.id.progress_bar
val SEEK_BAR = R.id.seek_bar
val TEXT_LAYOUT = R.id.text_layout
val TEXT = R.id.text
val BUTTON_POSITIVE = R.id.positive
val BUTTON_NEGATIVE = R.id.negative
val BUTTON_NEUTRAL = R.id.neutral


fun createDialog(context: Context, parent: ViewGroup? = null): View =
    LayoutInflater.from(context).inflate(R.layout.dialog, parent, false)

fun View.setDialogTitle(@StringRes resId: Int): View =
    this.setDialogTitle(context.getString(resId))

fun View.setDialogTitle(string: String): View {
    findViewById<TextView>(TITLE).apply {
        text = string
        visibility = View.VISIBLE
    }
    return this
}

fun View.setDialogMessage(@StringRes resId: Int): View =
    this.setDialogMessage(context.getString(resId))


fun View.setDialogMessage(string: String): View {
    findViewById<TextView>(MESSAGE).apply {
        text = string
        visibility = View.VISIBLE
    }
    return this
}


fun View.setDialogButton(id: Int, @StringRes resId: Int, action: ((View) -> Unit)?): View =
    this.setDialogButton(id, context.getString(resId), action)


fun View.setDialogButton(id: Int, string: String, action: ((View) -> Unit)?): View {
    findViewById<TextView>(id).apply {
        setOnClickListener(action)
        text = string
        visibility = View.VISIBLE
    }
    return this
}

fun View.setPositiveButton(@StringRes resId: Int, action: ((View) -> Unit)?): View =
    setDialogButton(BUTTON_POSITIVE, resId, action)

fun View.setNegativeButton(@StringRes resId: Int, action: ((View) -> Unit)?): View =
    setDialogButton(BUTTON_NEGATIVE, resId, action)

fun View.setNeutralButton(@StringRes resId: Int, action: ((View) -> Unit)?): View =
    setDialogButton(BUTTON_NEUTRAL, resId, action)


fun View.setDialogItems(list: ArrayList<String>, action: ((Int) -> Unit)? = null): View {
    setDialogList { recycler ->
        recycler.adapter = BasicRecyclerAdapter({ it }, list).also {
            it.onItemClicked = { item ->
                action?.let { it(list.indexOf(item)) }
            }
        }
    }
    return this
}

fun View.setDialogList(todo: ((RecyclerView) -> Unit)? = null): View {
    findViewById<RecyclerView>(LIST).apply {
        visibility = View.VISIBLE
        todo?.let { it(this) }
    }
    return this
}

fun View.setDialogImage(todo: ((ImageView) -> Unit)? = null): View {
    findViewById<ImageView>(IMAGE).apply {
        visibility = View.VISIBLE
        todo?.let { it(this) }
    }
    return this
}

fun View.getProgressBar(): ProgressBar = findViewById(PROGRESS_BAR)

fun View.enableProgressBar(state: Boolean = true): View {
    getProgressBar().visibility = if (state) View.VISIBLE else View.GONE
    return this
}

fun View.getSeekBar(): SeekBar = findViewById(SEEK_BAR)

fun View.enableSeekBar(state: Boolean = true): View {
    getSeekBar().visibility = if (state) View.VISIBLE else View.GONE
    return this
}

fun View.getText(): TextInputEditText = findViewById(TEXT)

fun View.getTextLayout(): TextInputLayout = findViewById(TEXT_LAYOUT)

fun View.enableTextLayout(state: Boolean = true): View {
    getTextLayout().visibility = if (state) View.VISIBLE else View.GONE
    return this
}


fun View.showButtons(state: Boolean = true): View {
    val visibility = if (state) View.VISIBLE else View.GONE
    findViewById<TextView>(BUTTON_POSITIVE).visibility = visibility
    findViewById<TextView>(BUTTON_NEGATIVE).visibility = visibility
    findViewById<TextView>(BUTTON_NEUTRAL).visibility = visibility
    return this
}

fun View.setNegativeCancel(dialog: Dialog): View =
    setNegativeButton(R.string.close) { dialog.dismiss() }
