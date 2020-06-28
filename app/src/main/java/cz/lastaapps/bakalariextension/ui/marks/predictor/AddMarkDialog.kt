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

package cz.lastaapps.bakalariextension.ui.marks.predictor

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.databinding.MarksPredictorAddMarkBinding
import kotlin.math.max

class AddMarkDialog {

    companion object {

        fun build(context: Context, onClick: ((mark: Mark) -> Unit), mark: Mark): AlertDialog {

            val bind = DataBindingUtil.inflate<MarksPredictorAddMarkBinding>(
                LayoutInflater.from(context), R.layout.marks_predictor_add_mark, null, false
            )

            bind.markData = mark

            val marksArray = arrayOf("1", "1-", "2", "2-", "3", "3-", "4", "4-", "5")
            bind.marksSpinner.adapter =
                ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, marksArray)

            if (!mark.isPoints) {
                bind.marksSpinner.setSelection(max(0, marksArray.indexOf(mark.markText)))
            }

            val dialog = AlertDialog.Builder(context).apply {
                setCancelable(true)
                setView(bind.root)
            }.create()

            bind.button.setOnClickListener {
                try {
                    val markPoint = bind.points.text.toString()

                    val markString =
                        if (!mark.isPoints)
                            bind.marksSpinner.selectedItem.toString()
                        else {
                            if (markPoint == "")
                                throw java.lang.Exception()
                            markPoint
                        }

                    val marksTotal = Integer.parseInt(bind.pointsTotal.text.toString())
                    val weight =
                        if (mark.weight != null)
                            Integer.parseInt(bind.weight.text.toString())
                        else null


                    val newMark = Mark(
                        mark.markDate,
                        mark.editDate,
                        mark.caption,
                        mark.theme,
                        markString,
                        mark.teacherId,
                        mark.type,
                        mark.typeNote,
                        weight,
                        mark.subjectId,
                        mark.isNew,
                        mark.isPoints,
                        mark.calculatedMarkText,
                        mark.classRankText,
                        mark.id,
                        markPoint,
                        marksTotal
                    )


                    dialog.dismiss()
                    onClick(newMark)
                } catch (e: Exception) {
                    Toast.makeText(
                        it.context, R.string.marks_predictor_enter_valid_data, Toast.LENGTH_LONG
                    ).show()
                }
            }

            return dialog
        }
    }
}