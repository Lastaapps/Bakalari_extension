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

package cz.lastaapps.bakalariextension.api.web

import android.util.Log
import cz.lastaapps.bakalariextension.api.web.data.WebModule
import cz.lastaapps.bakalariextension.tools.getOrNull
import org.json.JSONArray
import org.json.JSONObject

class WebModulesParser {

    companion object {

        private val TAG = WebModulesParser::class.java.simpleName

        fun parse(jsonObject: JSONObject): WebRoot {

            Log.i(TAG, "Parsing web modules")

            return WebRoot(
                parseWebModules(jsonObject.getJSONArray("WebModules")),
                parseWebModule(jsonObject.getJSONObject("Dashboard"))
            )
        }

        private fun parseWebModules(array: JSONArray): List<WebModule> {
            val list = ArrayList<WebModule>()

            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                list.add(parseWebModule(json))
            }

            return list
        }

        private fun parseWebModule(json: JSONObject): WebModule {
            return WebModule(
                json.getOrNull("IconId"),
                null,//unknown json.getOrNull("SubMenu")
                json.getOrNull("Url"),
                json.getOrNull("Name"),
            )
        }
    }
}