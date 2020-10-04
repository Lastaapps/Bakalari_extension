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

package cz.lastaapps.bakalariextension.ui.themes

import android.content.Context
import cz.lastaapps.bakalariextension.CurrentUser
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.themes.ThemesRepository
import cz.lastaapps.bakalariextension.api.themes.data.ThemeList
import cz.lastaapps.bakalariextension.ui.RefreshableListViewModel

/**Holds and loads Theme data for subject given*/
class ThemeViewModel(private val subjectId: String) :
    RefreshableListViewModel<ThemeList, ThemesRepository>(
        TAG,
        CurrentUser.requireDatabase().themesRepository.getRepository(subjectId)
    ) {

    companion object {
        private val TAG = ThemeViewModel::class.java.simpleName
    }

    override val data by lazy { repo.getThemes().asLiveData() }

    init {
        addEmptyObserver()
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.subject_no_theme)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.subject_theme_failed_to_load)
    }
}