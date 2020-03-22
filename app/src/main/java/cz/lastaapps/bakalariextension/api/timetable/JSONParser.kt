package cz.lastaapps.bakalariextension.api.timetable

import android.util.Log
import cz.lastaapps.bakalariextension.api.timetable.data.*
import cz.lastaapps.bakalariextension.api.timetable.data.TTData.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JSONParser {
    companion object {
        private val TAG = JSONParser::class.java.simpleName

        fun parseJson(root: JSONObject): Week {

            Log.i(TAG, "Parsing timetable json")

            return Week(
                parseHours(root.getJSONArray("Hours")),
                parseDays(root.getJSONArray("Days")),
                parseClasses(root.getJSONArray("Classes")),
                parseGroups(root.getJSONArray("Groups")),
                parseSubjects(root.getJSONArray("Subjects")),
                parseTeachers(root.getJSONArray("Teachers")),
                parseRooms(root.getJSONArray("Rooms")),
                parseCycles(root.getJSONArray("Cycles"))
            )
        }

        private fun parseHours(jsonArray: JSONArray): DataIdList<Hour> {
            val list = DataIdList<Hour>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Hour(
                    json.getInt("Id"),
                    json.getString("Caption"),
                    json.getString("BeginTime"),
                    json.getString("EndTime")
                )

                list.add(item)
            }

            list.sort()
            return list
        }

        private fun parseDays(jsonArray: JSONArray): ArrayList<Day> {
            val list = ArrayList<Day>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Day(
                    json.getInt("DayOfWeek"),
                    json.getString("Date"),
                    json.getString("DayDescription"),
                    json.getString("DayType"),
                    parseLessons(json.getJSONArray("Atoms"))
                )

                list.add(item)
            }

            return list
        }

        private fun parseLessons(jsonArray: JSONArray): DataIdList<Lesson> {
            val list = DataIdList<Lesson>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val groupIds = ArrayList<String>()
                val cycleIds = ArrayList<String>()
                val homeworkIds = ArrayList<String>()

                val loadUp = {
                    key: String, l: ArrayList<String> ->
                    val array = json.getJSONArray(key)

                    for (j in 0 until array.length()) {
                        l.add(array.getString(j))
                    }
                }

                loadUp("GroupIds", groupIds)
                loadUp("CycleIds", cycleIds)
                loadUp("HomeworkIds", homeworkIds)

                val change: JSONObject? = if (
                !json.isNull("Change")) {
                    json.getJSONObject("Change")
                } else {
                    null
                }

                val item = Lesson(
                    json.getInt("HourId"),
                    groupIds,
                    saveJson(json, "SubjectId"),
                    saveJson(json, "TeacherId"),
                    saveJson(json, "RoomId"),
                    cycleIds,
                    parseChange(change),
                    homeworkIds,
                    saveJson(json, "Theme")
                )

                list.add(item)
            }

            return list
        }

        private fun parseChange(json: JSONObject?): Change? {
            if (json == null) return null

            return Change(
                saveJson(json, "ChangeSubject"),
                json.getString("Day"),
                json.getString("Hours"),
                json.getString("ChangeType"),
                saveJson(json, "description"),
                json.getString("Time"),
                json.getString("TypeAbbrev"),
                json.getString("TypeName")
            )
        }

        private fun parseClasses(jsonArray: JSONArray): DataIdList<Class> {
            val list = DataIdList<Class>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Class(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseGroups(jsonArray: JSONArray): DataIdList<Group> {
            val list = DataIdList<Group>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Group(
                    json.getString("ClassId"),
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseSubjects(jsonArray: JSONArray): DataIdList<Subject> {
            val list = DataIdList<Subject>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Subject(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseTeachers(jsonArray: JSONArray): DataIdList<Teacher> {
            val list = DataIdList<Teacher>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Teacher(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseRooms(jsonArray: JSONArray): DataIdList<Room> {
            val list = DataIdList<Room>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Room(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseCycles(jsonArray: JSONArray): DataIdList<Cycle> {
            val list = DataIdList<Cycle>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Cycle(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            list.sort()
            return list
        }

        /**try to get String for the key given, protection again JSONException
         * and replacing null object with ""*/
        private fun saveJson(json: JSONObject, key: String): String {
            return try {
                return json.getString(key) ?: ""
            } catch (e: JSONException) {
                ""
            }
        }
    }
}
/*var toReturn: Week? = null

Log.i(TAG, "Parsing titables json")

var json = j!!.getJSONObject("results")
if (json.getString("result").toInt() != 1)
    return null

json = json.getJSONObject("rozvrh")

val days: ArrayList<Day> = ArrayList()
val patterns: ArrayList<LessonPattern> = ArrayList()
toReturn = Week(
    json.getString("kodcyklu"),
    json.getString("nazevcyklu"),
    json.getString("zkratkacyklu"),
    json.getString("typ"),
    patterns,
    days
)

val jPatterns = json.getJSONObject("hodiny").getJSONArray("hod")
for (i in 0 until jPatterns.length()) {
    val pJson = jPatterns.getJSONObject(i)
    patterns.add(
        LessonPattern(
            pJson.getString("begintime"),
            pJson.getString("endtime"),
            pJson.getString("caption")
        )
    )
}

val jDay = json.getJSONObject("dny").getJSONArray("den")
for (i in 0 until jDay.length()) {
    val lessons: ArrayList<Lesson> = ArrayList()

    val dJson = jDay.getJSONObject(i)
    days.add(
        Day(
            dJson.getString("datum"),
            dJson.getString("zkratka"),
            lessons
        )
    )

    val jLess = dJson.getJSONObject("hodiny").getJSONArray("hod")
    for (j in 0 until jLess.length()) {
        val lJson = jLess.getJSONObject(j)

        val id = getSecureString(lJson, "idcode")
        val type = getSecureString(lJson, "typ")
        val name = getSecureString(lJson, "nazev")//absence only
        val shortcut = getSecureString(lJson, "zkratka")//absence only
        val subject = getSecureString(lJson, "pr")
        val subjectShortcut = getSecureString(lJson, "zkrpr")
        val teacher = getSecureString(lJson, "uc")
        val teacherShortcut = getSecureString(lJson, "zkruc")
        val room = getSecureString(lJson, "mist")
        val roomShortcut = getSecureString(lJson, "zkrmist")
        val absence = getSecureString(lJson, "abs")
        val absenceShortcut = getSecureString(lJson, "zkrabs")
        val theme = getSecureString(lJson, "tema")
        val group = getSecureString(lJson, "skup")
        val groupShortcut = getSecureString(lJson, "zkrskup")
        val cycle = getSecureString(lJson, "cycle")
        val freed = getSecureString(lJson, "uvol")
        val change = getSecureString(lJson, "chng")
        val caption = getSecureString(lJson, "caption")
        val notice = getSecureString(lJson, "notice")

        lessons.add(
            Lesson(
                id,
                type,
                name,
                shortcut,
                subject,
                subjectShortcut,
                teacher,
                teacherShortcut,
                room,
                roomShortcut,
                absence,
                absenceShortcut,
                theme,
                group,
                groupShortcut,
                cycle,
                freed,
                change,
                caption,
                notice
            )
        )
    }

}

return toReturn*/