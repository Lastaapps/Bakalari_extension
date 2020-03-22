package cz.lastaapps.bakalariextension.api.timetable.data

open class DataID<T>(var id: T)

open class TTData(
    id: String,
    var shortcut: String,
    var name: String
): DataID<String>(id) {

    override fun equals(other: Any?): Boolean {
        return if (other is TTData)
            id == other.id
        else
            false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "id=$id name=$name shortcut=$shortcut"
    }

    class Room(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Teacher(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Subject(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Group(var classId: String, id: String, shortcut: String, name: String):
        TTData(id, shortcut, name) {}
    class Class(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Cycle(id: String, shortcut: String, name: String) :
        TTData(id, shortcut, name), Comparable<Cycle> {
        override fun compareTo(other: Cycle): Int {
            return id.compareTo(other.id)
        }

    }
}