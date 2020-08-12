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

package cz.lastaapps.bakalariextension.ui

import android.text.method.MovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("android:src")
fun setImageResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}

@BindingAdapter("android:movementMethod")
fun setMovementMethod(textView: TextView, movementMethod: MovementMethod) {
    textView.movementMethod = movementMethod
}

/*
//Two way data binding for ViewPager2 - not working correctly - position set before adapter attached
@BindingAdapter("currentItem")
fun setViewPager2Position(pager: ViewPager2, index: Int) {
    if (pager.currentItem != index)
        pager.setCurrentItem(index, false)
}

@InverseBindingAdapter(attribute = "currentItem")
fun getViewPager2Position(pager: ViewPager2): Int {
    return pager.currentItem
}

@BindingAdapter("currentItemAttrChanged")
fun setViewPager2Listener(pager: ViewPager2, attrChange: InverseBindingListener) {
    pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            pager.adapter?.let {
                attrChange.onChange()
            }
        }
    })
}
*/

