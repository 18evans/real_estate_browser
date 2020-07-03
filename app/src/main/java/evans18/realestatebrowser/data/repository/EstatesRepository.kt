package evans18.realestatebrowser.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import evans18.realestatebrowser.data.model.Estate

/**
 * Singleton responsible for being Single Source of Truth for storing the data store
 * of the Estate collection.
 * todo Note: ideally this would be a dependency-injected class with a singleton lifecycle across the application context
 */
object EstatesRepository {

    /**
     * Mapping by Estate's id as entity key.
     */
    private val mapEstates = MutableLiveData<MutableMap<Int, Estate>>().apply {
        value = mutableMapOf() //init with empty map
    }

    /**
     * Gets the unique estate objects.
     */
    val setEstate: LiveData<Set<Estate>> = Transformations.map(mapEstates) {
        it.values.toSet()
    }

    /**
     * Adds new items to collection, if not already contained
     */
    fun insert(vararg newEstates: Estate) {
        val newMap = mapEstates.value!!.apply {
            putAll(
                newEstates.map {
                    it.id to it
                }
            ) //puts all new mappings, if mapping for key exists, overwrite it
        }
        this.mapEstates.postValue(newMap) //use "post" in case method is accessed from separate thread
    }

}
