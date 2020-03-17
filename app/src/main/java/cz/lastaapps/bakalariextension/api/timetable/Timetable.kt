package cz.lastaapps.bakalariextension.api.timetable

import android.util.Log
import cz.lastaapps.bakalariextension.api.Login
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.LessonPattern
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.App
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class Timetable {

    companion object {
        private val TAG = Timetable::class.java.simpleName

        fun loadTimetable(cal: Calendar, forceReload: Boolean = false): Week? {

            TTTools.toMidnight(TTTools.toMonday(cal))

            var toReturn: Week? = null

            if (forceReload || TTStorage.lastUpdated(cal) == null) {
                toReturn = loadFromServer(cal)
            } else {
                if (TTStorage.lastUpdated(cal)!!.time.time
                    > Calendar.getInstance().time.time - TTTools.DAY
                ) {
                    toReturn = loadFromStorage(cal)
                }
                if (toReturn == null) {
                    toReturn = loadFromServer(cal)
                }
            }

            return toReturn
        }

        fun loadFromServer(cal: Calendar): Week? {
            try {
                Log.i(TAG, "Loading timetable from server - ${TTTools.format(cal)}")

                TTTools.toMidnight(TTTools.toMonday(cal))

                val schoolUrl = LoginData.get(LoginData.SP_URL)
                val token = LoginData.getToken()

                val pm = if (Login.get(Login.ROLE) == Login.ROLE_TEACHER)
                    "ucitelrozvrh" else "rozvrh"

                val date = if (cal != TTTools.PERMANENT)
                    TTTools.format(cal)
                else
                    "perm"

                val url = URL("$schoolUrl?hx=$token&pm=$pm&pmd=$date")
                val urlConnection = url.openConnection() as HttpURLConnection
                val input = urlConnection.inputStream


                Log.i(TAG, "Server: ${urlConnection.responseCode} ${urlConnection.responseMessage}")

                val json = xmlToJson(input)
                val week = parseJson(json!!)

                week?.let {
                    TTStorage.save(week.date, json)
                    TTNotifiService.startService(App.context)
                }

                return week

            } catch (e: Exception) {
                return null
            }
        }

        fun loadFromStorage(cal: Calendar): Week? {
            Log.i(TAG, "Loading timetable from storage - ${TTTools.format(cal)}")

            TTTools.toMidnight(TTTools.toMonday(cal))

            return try {
                parseJson(TTStorage.load(cal)!!)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }

        //https://github.com/smart-fun/XmlToJson
        private fun xmlToJson(inXml: InputStream): JSONObject? {
            val xmlToJson = XmlToJson.Builder(inXml, null).build()
            inXml.close()
            return xmlToJson.toJson()
        }

        private fun parseJson(j: JSONObject?): Week? {
            var toReturn: Week? = null

            Log.i(TAG, "Parsing timetables json")

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

            return toReturn
        }

        private fun getSecureString(json: JSONObject, key: String): String {
            return try {
                json.getString(key)
            } catch (e: JSONException) {
                ""
            }
        }
    }
}