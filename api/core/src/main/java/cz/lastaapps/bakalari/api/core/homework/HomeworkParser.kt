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

package cz.lastaapps.bakalari.api.core.homework

import android.util.Log
import cz.lastaapps.bakalari.api.core.DataIdList
import cz.lastaapps.bakalari.api.core.SimpleData
import cz.lastaapps.bakalari.api.core.attachment.holders.Attachment
import cz.lastaapps.bakalari.api.core.homework.holders.Homework
import cz.lastaapps.bakalari.api.core.homework.holders.HomeworkList
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.ZonedDateTime

object HomeworkParser {
    private val TAG = HomeworkParser::class.java.simpleName

    /**Parses marks from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
    fun parseJson(root: JSONObject): HomeworkList {

        Log.i(TAG, "Parsing homework json")

        //parses whole json
        return HomeworkList(
            parseHomework(
                root.getJSONArray(
                    "Homeworks"
                )
            ).sorted().reversed()
        )
    }

    /**parse array in /Homework */
    private fun parseHomework(jsonArray: JSONArray): HomeworkList {
        val list = HomeworkList()

        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)

            val item = Homework(
                safeJson(json, "ID"),
                parseDate(safeJson(json, "DateStart"))!!,
                parseDate(safeJson(json, "DateEnd"))!!,
                safeJson(json, "Content"),
                safeJson(json, "Notice"),
                json.getBoolean("Done"),
                json.getBoolean("Closed"),
                json.getBoolean("Electronic"),
                json.getBoolean("Finished"),
                json.getInt("Hour"),
                parseSimple(json.getJSONObject("Class")),
                parseSimple(json.getJSONObject("Group")),
                parseSimple(json.getJSONObject("Subject")),
                parseSimple(json.getJSONObject("Teacher")),
                parseAttachments(json.getJSONArray("Attachments"))
            )

            list.add(item)
        }

        return list
    }

    private fun parseDate(date: String): ZonedDateTime? {
        if (date == "") return null
        return TimeTools.parse(date, TimeTools.COMPLETE_FORMAT).toCommon()
    }


    /**parse data in /Homework/(Class, Group, Subject, Teacher) */
    private fun parseSimple(json: JSONObject): SimpleData {

        return SimpleData(
            json.getString("Id"),
            json.getString("Abbrev").trim(),
            json.getString("Name")
        )
    }

    /**loads homework's attachments*/
    private fun parseAttachments(array: JSONArray): DataIdList<Attachment> {
        val list = DataIdList<Attachment>()

        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)

            list.add(
                Attachment(
                    safeJson(json, "Id"),
                    safeJson(json, "Name"),
                    safeJson(json, "Type"),
                    json.getLong("Size")
                )
            )
        }
        return list
    }

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