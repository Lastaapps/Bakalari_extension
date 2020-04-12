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

package cz.lastaapps.bakalariextension.tools

import android.content.Context
import android.os.Looper

/**Shows toast from not Main thread*/
@Deprecated(
    "Dirty practise",
    ReplaceWith("Handler or Coroutines"),
    DeprecationLevel.WARNING
)
class MyToast {

    companion object {
        private val TAG = MyToast::class.java.simpleName

        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

    }
    /**Creates Toast object*/
    class makeText {
        private var context: Context
        private var res: Int = 0
        private var string: String = ""
        private var length: Int = 0

        constructor(context: Context, res: Int, length: Int) {
            this.context = context
            this.res = res
            this.length = length
        }

        constructor(context: Context, string: String, length: Int) {
            this.context = context
            this.string = string
            this.length = length
        }

        /**Shows Toast*/
        fun show() {
            if (string == "")
                android.os.Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, res, length).show()
                }
            else
                android.os.Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, string, length).show()
                }
        }
    }
}