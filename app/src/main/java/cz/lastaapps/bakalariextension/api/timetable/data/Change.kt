package cz.lastaapps.bakalariextension.api.timetable.data

data class Change(
    var subject: String,
    var day: String,
    var hours: String,
    var changeType: String,
    var description: String,
    var time: String,
    var typeShortcut: String,
    var typeName: String
) {
    /**Absence, normal lesson is canceled because of this*/
    fun isCanceled(): Boolean {
        return changeType == "Canceled"
    }

    /**New lesson in timetable*/
    fun isAdded(): Boolean {
        return changeType == "Added"
    }

    /**Empty spot in timetable*/
    fun isRemoved(): Boolean {
        return changeType == "Removed"
    }

}