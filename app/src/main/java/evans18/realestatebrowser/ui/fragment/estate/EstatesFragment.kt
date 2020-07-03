package evans18.realestatebrowser.ui.fragment.estate

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import com.google.android.material.appbar.AppBarLayout
import evans18.realestatebrowser.R
import evans18.realestatebrowser.data.model.Estate
import evans18.realestatebrowser.data.model.local.LocationModel
import evans18.realestatebrowser.data.model.local.Resource
import evans18.realestatebrowser.factory.NetworkRequestViewModelFactory
import evans18.realestatebrowser.ui.fragment.estate.detail.EstateDetailFragment.Companion.ARG_SELECTED_ESTATE_ID
import kotlinx.android.synthetic.main.fragment_estates.*

/**
 * Listing of estates.
 */
class EstatesFragment : Fragment() {

    /**
     * Latest text entered into [evans18.realestatebrowser.ui.view.CustomSearchView]
     * via [evans18.realestatebrowser.ui.view.CustomSearchView.setOnQueryTextListener].
     * Maintained so that Activity reapplies filtering during activity lifecycle restart actions (ie. minimize, reopen).
     */
    private var filterText = ""

    /**
     * Todo: Limit the view model only to the nav_graph where location is needed (ie. "by [navGraphViewModels]")
     * To do this put Estates & EstateDetail fragments in their own nav graph
     * (warning: currently this causes bottom nav button between about & estates to not work. todo: figure out why).
     * Once that is done consider using "by [navGraphViewModels]" passing as parameter that nav graph's id.
     */
    private val locationViewModel: LocationViewModel by activityViewModels()

    /**
     * Bind to owner activity because I don't want data to be recalled every time
     * user gets back to fragment (see EstatesViewModel.init()).
     */
    private val estatesViewModel: EstatesViewModel by activityViewModels {
        NetworkRequestViewModelFactory
    }

    private val adapter by lazy {
        EstatesFilterableRecyclerViewAdapter(
            { estateId ->
                //todo confirm whether actual id of a loaded estate
                estatesViewModel.promptEstateClicked(estateId)
            },
            onAdapterIsEmptyListener = { isEmpty ->
                view_search_no_results.isVisible = isEmpty && filterText.isNotBlank() //also only during actual search
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_estates, container, false)
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        recycler_view.adapter = adapter
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private fun handleQuery(query: String) {
                this@EstatesFragment.filterText = query
                adapter.applyFilterByQuery(query)
                recycler_view.scrollToPosition(0)
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                handleQuery(query!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                handleQuery(newText!!)
                return true
            }
        })
        search_view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                requireActivity().findViewById<AppBarLayout>(R.id.app_bar).setExpanded(false)
        }

        bindObservables()
    }

    private fun bindObservables() {
        fun setShowingEstateListLoadingIndicator(isShowing: Boolean) = if (isShowing) shimmer_view_container.isVisible = true else {
            shimmer_view_container.isVisible = false
            shimmer_view_container.stopShimmer()
        }

        estatesViewModel.run {
            estateListRequestObservable.observe(viewLifecycleOwner, Observer { resListEstates ->
                when (resListEstates) {
                    is Resource.Loading -> setShowingEstateListLoadingIndicator(true)
                    is Resource.Error -> setShowingEstateListLoadingIndicator(false)
                    is Resource.Success -> setShowingEstateListLoadingIndicator(false) //note: list is populated from setDistanceableEstates's Observer
                }
            })
            setDistanceableEstates.observe(viewLifecycleOwner, Observer { setEstates ->
                adapter.replaceAll(setEstates, this@EstatesFragment.filterText)
            })
            navigateToDetails.observe(viewLifecycleOwner, Observer { eventEstateSelected ->
                eventEstateSelected.getContentIfNotHandled()?.let { estateId ->// Only proceed if the event has never been handled
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.action_navigation_estates_to_estate_details, Bundle().apply {
                            putInt(ARG_SELECTED_ESTATE_ID, estateId)
                        })
                }

            })

        }
    }

    /**
     * Observable for a [Set] of [EstateWrapper] objects to be listed in the [EstatesFilterableRecyclerViewAdapter].
     * Wrapped is the [Estate] along with optional properties that may be visualised in
     * the [EstatesFilterableRecyclerViewAdapter] such as [EstateWrapper.distanceFromUser].
     */
    private val setDistanceableEstates by lazy {
        MediatorLiveData<Set<EstateWrapper>>().apply {
            var setEstates: Set<Estate>? = null
            var userLocation: LocationModel? = null

            /**
             * @return - distance in meters.
             * @throws NullPointerException if any of the items is not initialized.
             */
            fun getDistanceFromUserToEstate(estate: Estate): Float {
                val arrResults = FloatArray(1) //array's contents are written to

                Location.distanceBetween(
                    userLocation!!.latitude,
                    userLocation!!.longitude,
                    estate.latitude,
                    estate.longitude,
                    arrResults
                )
                return arrResults[0]
            }

            /**
             * If the states have loaded sets the value using [MediatorLiveData.setValue].
             * If user location is not null, the wrappers' [EstateWrapper.distanceFromUser] property is
             * initialized from a performed calculation of the the distance between the geographical location
             * of the estate as defined by [Estate.latitude] & [Estate.longitude] and the user's location.
             *
             * Note: Expected to be prompted during the [Observer] callback of each of the
             */
            fun onSourceUpdated() {
                setEstates?.run {
                    map { estate ->
                        EstateWrapper(estate, userLocation?.let {
                            getDistanceFromUserToEstate(estate)
                        })
                    }.toSet().let {
                        this@apply.postValue(it)
                    }

                }
            }

            addSource(estatesViewModel.estateListRequestObservable) { resEstates ->
                if (resEstates is Resource.Success) {
                    setEstates = resEstates.data!!
                    onSourceUpdated()
                }
            }
            addSource(locationViewModel.lastUserLocation) { location ->
                userLocation = location
                onSourceUpdated()
            }
        }
    }

}