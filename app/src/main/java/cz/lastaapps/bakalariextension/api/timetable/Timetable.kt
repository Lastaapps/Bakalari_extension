package cz.lastaapps.bakalariextension.api.timetable

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime


class Timetable {

    companion object {
        private val TAG = Timetable::class.java.simpleName

        fun loadTimetable(cal: ZonedDateTime, forceReload: Boolean = false): Week? {

            val time = TimeTools.toMidnight(TimeTools.toMonday(cal))

            var toReturn: Week? = null

            if (forceReload || TTStorage.lastUpdated(time) == null) {
                toReturn = loadFromServer(time)
            } else {
                val lastUpdated = TTStorage.lastUpdated(time)
                if (lastUpdated != null)
                    if (lastUpdated.isAfter(TimeTools.now.minusDays(1))
                        || cal == TimeTools.PERMANENT
                    ) {
                        toReturn = loadFromStorage(time)
                    }

                if (toReturn == null) {
                    toReturn = loadFromServer(time)
                }
            }

            return toReturn
        }

        fun loadFromServer(cal: ZonedDateTime): Week? {
            try {
                Log.i(
                    TAG,
                    "Loading timetable from server - ${TimeTools.format(
                        cal,
                        TimeTools.DATE_FORMAT
                    )}"
                )

                val json = (
                        if (cal != TimeTools.PERMANENT)
                            ConnMgr.serverGet(
                                "timetable/actual?date=${TimeTools.format(
                                    cal,
                                    TimeTools.DATE_FORMAT
                                )}"
                            )
                        else
                            ConnMgr.serverGet("timetable/permanent")
                        ) ?: return null

                val week = JSONParser.parseJson(json)

                TTStorage.save(cal, json)
                TTNotifiService.startService(App.context)

                return week

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        fun loadFromStorage(cal: ZonedDateTime): Week? {

            val time = TimeTools.toMidnight(
                TimeTools.toMonday(cal)
            )

            Log.i(
                TAG,
                "Loading timetable from storage - ${TimeTools.format(time, TimeTools.DATE_FORMAT)}"
            )

            return try {
                val json = TTStorage.load(time) ?: return null
                JSONParser.parseJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}