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

package cz.lastaapps.bakalari.app.ui.others

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalari.app.BuildConfig
import cz.lastaapps.bakalari.app.R
import java.util.*

/**In loadingActivity and HomeFragments
 * It's the main logo showing app's name and icon (and BETA stage of the app)*/
class LogoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //inflates views
        val view = inflater.inflate(R.layout.logo_label, container, false)

        //for beta users puts label BETA next to main Bakalari label
        if (BuildConfig.VERSION_NAME.toLowerCase(Locale.ROOT).contains("beta")) {
            view.findViewById<TextView>(R.id.beta).visibility = View.VISIBLE
        }

        return view
    }

}
