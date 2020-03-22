package cz.lastaapps.bakalariextension.api.timetable.data

class Lesson(
    hourId: Int,
    var groupIds: ArrayList<String>,
    var subjectId: String,
    var teacherId: String,
    var roomId: String,
    var cycleIds: ArrayList<String>,
    var change: Change?,
    var homeworkIds: ArrayList<String>,
    var theme: String
): DataID<Int>(hourId) {

    fun isNormal(): Boolean {
        if (change == null) return true
        if (change!!.isAdded()) return true
        return false
    }

    fun isRemoved(): Boolean {
        if (change == null) return false
        if (change!!.isRemoved()) return true
        return false
    }

    fun isAbsence(): Boolean {
        if (change == null) return false
        if (change!!.isCanceled()) return true
        return false
    }

}