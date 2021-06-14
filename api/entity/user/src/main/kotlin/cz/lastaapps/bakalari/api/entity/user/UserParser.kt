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

package cz.lastaapps.bakalari.api.entity.user

import android.util.Log
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import cz.lastaapps.bakalari.tools.getStringOrEmpty
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object UserParser {
    private val TAG = UserParser::class.java.simpleName

    /**Parses marks from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
    fun parseJson(root: JSONObject): User {

        Log.i(TAG, "Parsing json")

        //parses whole json
        return parseUser(root)
    }

    private fun parseUser(json: JSONObject): User {
        val userId = safeJson(json, "UserUID")

        return User(
            userId,
            json.getStringOrEmpty("CampaignCategoryCode"),
            parseSimple(json.getJSONObject("Class")),
            safeJson(json, "FullName"),
            safeJson(json, "SchoolOrganizationName"),
            safeJson(json, "SchoolType"),
            safeJson(json, "UserType"),
            safeJson(json, "UserTypeText"),
            json.getInt("StudyYear"),
            parseModules(userId, json.getJSONArray("EnabledModules")),
            parseSemester(json.getJSONObject("SettingModules").getJSONObject("Common"))
        )
    }

    /**parses all modules and rights*/
    private fun parseModules(uid: String, array: JSONArray): ModuleList {
        val list = ModuleList()

        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)

            val moduleName = json.getString("Module")
            val rightsArray = json.getJSONArray("Rights")

            for (j in 0 until rightsArray.length()) {
                list.add(ModuleFeature(uid, moduleName, rightsArray.getString(j)))
            }
        }

        return list
    }

    /**parses semester*/
    private fun parseSemester(json: JSONObject): Semester? {

        try {
            json.getJSONObject("ActualSemester").apply {
                return Semester(
                    getInt("SemesterId"),
                    parseDate(getString("From")),
                    parseDate(getString("To"))
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**parse simple data*/
    private fun parseSimple(json: JSONObject): SimpleData {

        return SimpleData(
            json.getString("Id"),
            json.getString("Abbrev").trim(),
            json.getString("Name")
        )
    }

    private fun parseDate(date: String) =
        TimeTools.parse(date, TimeTools.COMPLETE_FORMAT).toCommon()

    /**try to get String for the key given, protection again JSONException
     * and replacing null object with ""*/
    private fun safeJson(json: JSONObject, key: String): String {
        return try {
            if (!json.isNull(key))
                return json.getString(key)
            else
                ""
        } catch (e: JSONException) {
            ""
        }
    }
}