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

package cz.lastaapps.bakalari.app.ui.settings.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.google.android.material.bottomsheet.BottomSheetDialog
import cz.lastaapps.bakalari.app.ui.uitools.BasicRecyclerAdapter
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.createDialog
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setDialogList
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setDialogTitle
import cz.lastaapps.bakalari.app.ui.uitools.customdialog.setNegativeCancel


open class MyListPreference<T> : Preference {
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    private var mTitle = ""
    fun setDialogTitle(title: String) {
        mTitle = title
    }

    fun getDialogTitle(): String = mTitle

    private var mList: List<T> = emptyList()
    private var mConverter: ((T) -> String) = { it.toString() }
    private var mOnItemSelectedListener: ((T) -> Unit) = {}

    fun setUp(
        list: List<T> = mList,
        converter: ((T) -> String) = mConverter,
        onItemSelectedListener: ((T) -> Unit) = mOnItemSelectedListener,
    ) {
        mList = list
        mConverter = converter
        mOnItemSelectedListener = onItemSelectedListener

        setOnPreferenceClickListener {
            openDialog()
            true
        }
    }

    private fun openDialog() {

        val dialog = BottomSheetDialog(context)

        val adapter = BasicRecyclerAdapter(mConverter, mList)
        adapter.onItemClicked = {
            dialog.dismiss()
            mOnItemSelectedListener(it)
        }

        dialog.setContentView(
            createDialog(context)
                .setDialogTitle(mTitle)
                .setDialogList {
                    it.adapter = adapter
                }
                .setNegativeCancel(dialog))
        dialog.show()
    }
}

