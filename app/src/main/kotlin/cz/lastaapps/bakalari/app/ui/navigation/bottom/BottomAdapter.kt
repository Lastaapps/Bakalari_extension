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

package cz.lastaapps.bakalari.app.ui.navigation.bottom

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalari.app.R

/**Creates view to be shown in the BottomDialog*/
class BottomAdapter(var list: List<BottomItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    //menu item, id, position
    var onClick: ((Int, Int) -> Unit)? = null

    /**Creates a simple view with image and a text*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.botton_nav_item, parent, false)
        ) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val root = holder.itemView

        val image = root.findViewById<ImageView>(R.id.image)
        val text = root.findViewById<TextView>(R.id.text)

        val item = list[position]

        image.setImageResource(item.drawableResource)
        text.setText(item.title)

        root.setOnClickListener {
            onClick?.let {
                it(item.navId, position)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return list[position].navId.toLong()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}