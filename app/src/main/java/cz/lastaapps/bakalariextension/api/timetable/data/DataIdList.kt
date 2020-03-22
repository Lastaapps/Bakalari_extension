package cz.lastaapps.bakalariextension.api.timetable.data

/**Adds method to access data via their IDs*/
class DataIdList<T: DataID<*>>: ArrayList<T>() {

    fun getById(id: Any): T? {
        forEach {
            if (it.id == id)
                return it
        }
        return null
    }

    fun getAllById(id: Any): ArrayList<T> {
        val array = ArrayList<T>()
        forEach {
            if (it.id == id)
                array.add(it)
        }
        return array
    }
}