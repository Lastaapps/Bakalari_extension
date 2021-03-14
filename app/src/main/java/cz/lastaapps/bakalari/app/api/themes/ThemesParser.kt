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

package cz.lastaapps.bakalari.app.api.themes

import android.util.Log
import cz.lastaapps.bakalari.app.api.themes.data.Theme
import cz.lastaapps.bakalari.app.api.themes.data.ThemeList
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import org.json.JSONObject

object ThemesParser {
    private val TAG = ThemesParser::class.java.simpleName

    /**Parses themes json*/
    fun parseJson(root: JSONObject): ThemeList {
        Log.i(TAG, "Parsing theme json")

        val subjectID = root.getJSONObject("Subject").getString("Id")

        val list = cz.lastaapps.bakalari.app.api.themes.data.ThemeList()
        val jsonArray = root.getJSONArray("Themes")

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Theme(
                    subjectID,
                    TimeTools.parse(json.getString("Date"), TimeTools.COMPLETE_FORMAT).toCommon(),
                    json.getString("Theme"),
                    json.getString("Note"),
                    json.getString("HourCaption"),
                    json.getString("LessonLabel")
                )

                list.add(item)
            }

            return list
        }
    }
