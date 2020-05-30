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

package cz.lastaapps.bakalariextension.api.homework

import android.util.Log
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.attachment.data.Attachment
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class HomeworkParser {
    companion object {
        private val TAG = HomeworkParser::class.java.simpleName

        /**Parses marks from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
        fun parseJson(root: JSONObject): DataIdList<Homework> {

            Log.i(TAG, "Parsing homework json")

            //parses whole json
            return DataIdList(parseHomework(root.getJSONArray("Homeworks")).sorted().reversed())
        }

        /**parse array in /Homework */
        private fun parseHomework(jsonArray: JSONArray): DataIdList<Homework> {
            val list = DataIdList<Homework>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Homework(
                    safeJson(json, "ID"),
                    safeJson(json, "DateAward"),
                    safeJson(json, "DateControl"),
                    safeJson(json, "DateDone"),
                    safeJson(json, "DateStart"),
                    safeJson(json, "DateEnd"),
                    safeJson(json, "Content"),
                    safeJson(json, "Notice"),
                    json.getBoolean("Done"),
                    json.getBoolean("Closed"),
                    json.getBoolean("Electronic"),
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
                        json.getInt("Size")
                    )
                )
            }
            return list
        }

        /**try to get String for the key given, protection again JSONException
         * and replacing null object with ""*/
        private fun safeJson(json: JSONObject, key: String): String {
            return try {
                return json.getString(key) ?: ""
            } catch (e: JSONException) {
                ""
            }
        }

        /**Encodes homework list back to JSON*/
        fun encodeJson(list: List<Homework>): JSONObject {
            val homeworkList = JSONArray()

            for (homework in list) {
                homeworkList.put(encodeHomework(homework))
            }

            return JSONObject().apply {
                put("Homeworks", homeworkList)
            }
        }

        /**encodes Homework object to JSON*/
        private fun encodeHomework(h: Homework): JSONObject {
            return JSONObject().apply {
                put("ID", h.id)
                put("DateAward", h.dateAward)
                put("DateControl", h.dateControl)
                put("DateDone", h.dateDone)
                put("DateStart", h.dateStart)
                put("DateEnd", h.dateEnd)
                put("Content", h.content)
                put("Notice", h.notice)
                put("Done", h.done)
                put("Closed", h.closed)
                put("Electronic", h.electronic)
                put("Hour", h.hour)
                put("Class", encodeSimple(h.classInfo))
                put("Group", encodeSimple(h.group))
                put("Subject", encodeSimple(h.subject))
                put("Teacher", encodeSimple(h.teacher))
                put("Attachments", encodeAttachment(h.attachments))
            }
        }

        /**Encodes group, class, subject and teacher back to JSON*/
        private fun encodeSimple(data: SimpleData): JSONObject {
            return JSONObject().apply {
                put("Id", data.id)
                put("Abbrev", data.shortcut)
                put("Name", data.name)
            }
        }

        /**Encodes attachments back to JSONArray*/
        private fun encodeAttachment(attachments: DataIdList<Attachment>): JSONArray {
            val array = JSONArray()

            for (attachment in attachments) {
                array.put(JSONObject().apply {
                    put("Id", attachment.id)
                    put("Name", attachment.fileName)
                    put("Type", attachment.type)
                    put("Size", attachment.size)
                })
            }
            return array
        }
    }
}