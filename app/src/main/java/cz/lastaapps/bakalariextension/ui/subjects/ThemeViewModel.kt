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

package cz.lastaapps.bakalariextension.ui.subjects

import android.content.Context
import android.widget.Toast
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.subjects.SubjectLoader
import cz.lastaapps.bakalariextension.api.subjects.ThemeList
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**Holds and loads Theme data for subject given*/
class ThemeViewModel(private val subjectId: String) : RefreshableViewModel<ThemeList>() {

    override fun onRefresh(force: Boolean) {

        if (isRefreshing.value!!)
            return

        isRefreshing.value = true

        CoroutineScope(Dispatchers.Default).launch {

            val theme = SubjectLoader.loadThemesFromServer(subjectId)

            withContext(Dispatchers.Main) {

                failed.value = false
                isEmpty.value = false

                if (theme == null) {
                    if (data.value == null) {
                        failed.value = true
                    }
                    Toast.makeText(
                        App.context,
                        R.string.subject_theme_failed_to_load,
                        Toast.LENGTH_LONG
                    )
                        .show()
                } else {
                    isEmpty.value = theme.isEmpty()
                    data.value = theme
                }

                isRefreshing.value = false
            }
        }
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.subject_no_theme)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.subject_theme_failed_to_load)
    }


}