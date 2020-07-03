package evans18.realestatebrowser.ui.fragment.estate

import evans18.realestatebrowser.data.model.Estate

/**
 * Local wrapper for listed in [EstatesFilterableRecyclerViewAdapter] items.
 * The [Estate] object can be associated with a distance value, which specifies how far away
 * the [Estate.latitude] and [Estate.longitude] are from the current user's device.
 *
 * @param estate - wrapped item to be displayed in list.
 * @param distanceFromUser - distance in metres to estate from user's device. Can be null, if no distance data yet.
 */
data class EstateWrapper(val estate: Estate, val distanceFromUser: Float? = null) {
    fun contains(query: String, ignoreCase: Boolean = false) = estate.contains(query, ignoreCase)
}