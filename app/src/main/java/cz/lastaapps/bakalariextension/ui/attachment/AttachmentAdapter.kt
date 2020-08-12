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

package cz.lastaapps.bakalariextension.ui.attachment

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.attachment.data.Attachment

/** Displays attachments*/
class AttachmentAdapter(
    var list: List<Attachment> = ArrayList(),
    var onClick: ((Attachment) -> Unit)
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return object :
            RecyclerView.ViewHolder(inflater.inflate(R.layout.attachment_row, parent, false)) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        val attachment = list[position]

        view.setOnClickListener { onClick(attachment) }

        view.findViewById<TextView>(R.id.name).text = attachment.fileName
        view.findViewById<TextView>(R.id.size).text = if (attachment.size > 0)
            Formatter.formatShortFileSize(view.context, attachment.size) else ""
    }

    override fun getItemCount(): Int = list.size
}