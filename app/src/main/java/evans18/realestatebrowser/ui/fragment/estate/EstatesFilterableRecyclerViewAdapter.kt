package evans18.realestatebrowser.ui.fragment.estate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import evans18.realestatebrowser.R
import evans18.realestatebrowser.util.commaString
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.estates_list_item.*

class EstatesFilterableRecyclerViewAdapter(
    private val onItemClickListener: (Int) -> Unit,
    private val onAdapterIsEmptyListener: (Boolean) -> Unit
) : RecyclerView.Adapter<EstatesFilterableRecyclerViewAdapter.ViewHolder>() {

    private companion object {
        /**
         * Identifier for the [SortedListAdapterCallback.getChangePayload]'s [Pair] return result.
         * Which is [Pair.first] property of [Pair].
         */
        const val PAYLOAD_DISTANCE = "PAYLOAD_DISTANCE"
    }

    /**
     * All the items which the [filterableSortedData] is able to show.
     * This list holds the non-ordered (aka non-sorted) and non-filtered items.
     *
     * Used as a backing field in order to populate items back to the [filterableSortedData]
     * upon undoing of previously applied filters.
     */
    private var originalData = emptySet<EstateWrapper>()

    /**
     * Adapter callback used to represent the ruleset for which [RecyclerView] notify methods
     * must be called per change of the local list.
     */
    private val adapterCallback = object : SortedListAdapterCallback<EstateWrapper>(this) {
        override fun compare(o1: EstateWrapper, o2: EstateWrapper): Int {
            return o1.estate.price.compareTo(o2.estate.price)
        }

        override fun areContentsTheSame(oldItem: EstateWrapper, newItem: EstateWrapper): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(item1: EstateWrapper, item2: EstateWrapper): Boolean {
            return item1.estate.id == item2.estate.id
        }

        /**
         * Detects the difference in two items.
         * Note: Currently supports only finding difference for the [EstateWrapper.distanceFromUser] field.
         */
        override fun getChangePayload(oldItem: EstateWrapper, latestItem: EstateWrapper): Pair<String, Any?>? {
            return if (oldItem.distanceFromUser != latestItem.distanceFromUser) {
                return PAYLOAD_DISTANCE to latestItem.distanceFromUser
            } else null
        }

    }

    /**
     * List containing the actual visualised items adapting the collection with an Adapter honouring the
     * [RecyclerView.Adapter] "notify" actions per collection mutation operations.
     *
     * List supports sorting and filtering.
     */
    private val filterableSortedData: SortedList<EstateWrapper> = SortedList(EstateWrapper::class.java, adapterCallback)

    /**
     * Sets the viewable list ([filterableSortedData]) to only have the new data contained in the passed collection.
     * Makes the list mutation in an efficient manner using the batched update properties of [SortedList].
     *
     * Note: Current implementation of [SortedList.Callback.areItemsTheSame]
     * will not allow duplicates, meaning that [SortedList.addAll] consequently
     * will internally be handled to not add duplicate values of the passed collection.
     *
     * @param newData - items that are going to be present in the adapter's data.
     * @param filterText - items only matching this query will be present in the viewable list.
     */
    fun replaceAll(newData: Set<EstateWrapper>, filterText: String) {
        originalData = newData

        filterableSortedData.beginBatchedUpdates()
        // First: remove no longer present elements
        // reverse order, because mutability will occur during iteration
        // and in that case indices are very likely to get messed up
        for (i in filterableSortedData.size() - 1 downTo 0) {
            val item: EstateWrapper = filterableSortedData[i]
            // check if contained in new
            val isContained = newData.any { newItem ->
                adapterCallback.areItemsTheSame(newItem, item) //use same checking method as internal SortedList
            }
            //remove if not contained OR not matching filter
            if (!isContained || !item.contains(filterText, true))
                filterableSortedData.remove(item)
        }

        //Second: only the new items matching filter
        for (newItem in newData) {
            if (newItem.contains(filterText, true))
                filterableSortedData.add(newItem) //will internally find and update if exists (see [adapterCallback.areItemsTheSame])
        }
        filterableSortedData.endBatchedUpdates()
    }

    /**
     * Looks for items in the original collection ([originalData]) whether they match the passed query String.
     * If matching, adds the item to the viewable list ([filterableSortedData]), else removes them.
     */
    fun applyFilterByQuery(query: String) {
        filterableSortedData.beginBatchedUpdates()
        for (item in originalData) {
            if (item.contains(query, true))
                filterableSortedData.add(item) //note: will find and update if exists (see [adapterCallback.areItemsTheSame])
            else
                filterableSortedData.remove(item)
        }
        filterableSortedData.endBatchedUpdates()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.estates_list_item, parent, false))
    }

    /**
     * Checks if supported change payloads have been issued and attempts to consume them
     * doing a partial ViewHolder refresh if so.
     * If doesn't consume, propagates to full ViewHolder refresh.
     *
     * Note: Supported payload type are set at [SortedListAdapterCallback.getChangePayload] implementation of [adapterCallback].
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        @Suppress("UNCHECKED_CAST") val mapPayloads = (payloads as List<Pair<String, Any>>).toMap()

        //currently consume payload change only for distance payload
        mapPayloads[PAYLOAD_DISTANCE]?.let { meters -> //if has distance
            val newDistance = meters as Float? //cast value to the expected Type
            showDistanceInHolder(holder, newDistance)
            return //consume
        }

        //if payload not consumed, do full change
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.run { //expose view binding to layout (experimental feature)
            itemView.run {  //expose container
                val estateWrapper = filterableSortedData[position]
                val estate = estateWrapper.estate

                setOnClickListener {
                    onItemClickListener.invoke(estate.id)
                }

                context.apply { //expose context in scope
                    estate.let { item ->
                        item.image.let { bitmap ->
                            if (bitmap == null) {
                                //placeholder
                                image.setBackgroundColor(getColor(R.color.medium))
                                image.setImageDrawable(getDrawable(R.drawable.logo_github))
                            } else image.setImageBitmap(bitmap)
                        }
                        tv_price.text = getString(R.string.price_with_usd_currency, item.price.commaString)
                        postal_code_and_city.text = getString(R.string.postal_code_with_city, item.postalCode, item.city)
                        tv_count_bed.text = item.bedroomCount.toString()
                        tv_count_bathroom.text = item.bathroomCount.toString()
                        tv_size.text = item.size.toString()
                        //todo distance

                        showDistanceInHolder(holder, estateWrapper.distanceFromUser)
                    }
                }

            }
        }

    }

    /**
     * @param meters - value to be shown. Pass null to hide distance.
     */
    private fun showDistanceInHolder(holder: ViewHolder, meters: Float?) {
        holder.tv_distance_from_me.isVisible = meters != null

        if (meters != null) {
            val km = meters / 1000
            holder.tv_distance_from_me.text = holder.itemView.context.getString(R.string.estate_details_distance_kilometers_from_user, km)
        }
    }

    override fun getItemCount() = filterableSortedData.size().also { size ->
        onAdapterIsEmptyListener.invoke(size == 0)
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

}