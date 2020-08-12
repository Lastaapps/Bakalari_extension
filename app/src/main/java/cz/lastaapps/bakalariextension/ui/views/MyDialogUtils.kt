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

package cz.lastaapps.bakalariextension.ui.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.BasicRecyclerAdapter

/**Holds the method for dialog_... layout files*/

const val BUTTON_POSITIVE = R.id.positive
const val BUTTON_NEGATIVE = R.id.negative
const val BUTTON_NEUTRAL = R.id.neutral

fun createDialog(context: Context, parent: ViewGroup? = null) =
    LayoutInflater.from(context).inflate(R.layout.dialog, parent, false)

fun View.setDialogTitle(resId: Int): View =
    this.setDialogTitle(context.getString(resId))

fun View.setDialogTitle(string: String): View {
    findViewById<TextView>(R.id.title).apply {
        text = string
        visibility = View.VISIBLE
    }
    return this
}

fun View.setDialogMessage(resId: Int): View =
    this.setDialogMessage(context.getString(resId))


fun View.setDialogMessage(string: String): View {
    findViewById<TextView>(R.id.message).apply {
        text = string
        visibility = View.VISIBLE
    }
    return this
}

fun View.setDialogButton(id: Int, resId: Int, action: ((View) -> Unit)?): View =
    this.setDialogButton(id, context.getString(resId), action)


fun View.setDialogButton(id: Int, string: String, action: ((View) -> Unit)?): View {
    findViewById<TextView>(id).apply {
        setOnClickListener(action)
        text = string
        visibility = View.VISIBLE
    }
    return this
}

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
    findViewById<RecyclerView>(R.id.list).apply {
        visibility = View.VISIBLE
        todo?.let { it(this) }
    }
    return this
}


fun View.setDialogImage(todo: ((ImageView) -> Unit)? = null): View {
    findViewById<ImageView>(R.id.image).apply {
        visibility = View.VISIBLE
        todo?.let { it(this) }
    }
    return this
}
