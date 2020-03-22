package cz.lastaapps.bakalariextension.api.timetable.data

class Hour (
    id: Int,
    var caption: String,
    var begin: String,
    var end: String
): Comparable<Hour>, DataID<Int>(id) {
    override fun compareTo(other: Hour): Int {
        return caption.toInt().compareTo(other.caption.toInt())
    }
}