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

package cz.lastaapps.bakalari.app.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridView

/**Supports wrap_content in ScrollView*/
class MyGridView : GridView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    //https://stackoverflow.com/questions/4523609/grid-of-images-inside-scrollview/4536955#4536955
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // HACK!  TAKE THAT ANDROID!
        if (layoutParams.height == LayoutParams.WRAP_CONTENT) {
            // Calculate entire height by providing a very large height hint.
            // View.MEASURED_SIZE_MASK represents the largest height possible.
            val expandSpec = MeasureSpec.makeMeasureSpec(
                View.MEASURED_SIZE_MASK,
                MeasureSpec.AT_MOST
            )
            super.onMeasure(widthMeasureSpec, expandSpec)
            /*val params: LayoutParams = layoutParams
            params.height = measuredHeight*/
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}