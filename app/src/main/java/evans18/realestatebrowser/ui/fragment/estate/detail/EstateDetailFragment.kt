package evans18.realestatebrowser.ui.fragment.estate.detail

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.CollapsingToolbarLayout
import evans18.realestatebrowser.R
import evans18.realestatebrowser.data.model.local.LocationModel
import evans18.realestatebrowser.ui.fragment.estate.LocationViewModel
import evans18.realestatebrowser.ui.fragment.estate.detail.map.NestedScrollViewMapFragment
import evans18.realestatebrowser.util.TAG
import evans18.realestatebrowser.util.commaString
import kotlinx.android.synthetic.main.fragment_estate_detail.*

class EstateDetailFragment : Fragment() {

    companion object {
        const val ARG_SELECTED_ESTATE_ID = "ARG_SELECTED_ESTATE_ID"
        const val ON_MAP_READY_ZOOM_LEVEL = 9f
    }

    /**
     * Map fragment initiated from map view within [Fragment.onCreateView]'s returned layout.
     */
    private val mapFragment by lazy {
        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    }

    /**
     * Todo: Limit the view model only to the nav_graph where location is needed (ie. "by [navGraphViewModels]")
     * To do this put Estates & EstateDetail fragments in their own nav graph
     * (warning: currently this causes bottom nav button between about & estates to not work. todo: figure out why).
     * Once that is done consider using "by [navGraphViewModels]" passing as parameter that nav graph's id.
     */
    private val locationViewModel: LocationViewModel by activityViewModels()

    private val viewModel: EstateDetailViewModel by viewModels()

    private val distanceChangedObservable by lazy {
        MediatorLiveData<Float>().apply {
            var estateLocation: LocationModel? = null
            var userLocation: LocationModel? = null

            /**
             * @return - distance in meters.
             * @throws NullPointerException if any of the items is not initialized.
             */
            fun getDistanceFromUserToEstate(): Float {
                val arrResults = FloatArray(1) //array's contents are written to

                Location.distanceBetween(
                    userLocation!!.latitude,
                    userLocation!!.longitude,
                    estateLocation!!.latitude,
                    estateLocation!!.longitude,
                    arrResults
                )
                return arrResults[0]
            }

            /**
             * Sets the distance between the two as the value to the wrapping [MediatorLiveData].
             *
             * Note: Expected to be prompted during the [Observer] callback of each of the
             */
            fun onSourceUpdated() {
                if (estateLocation != null && userLocation != null) {
                    value = getDistanceFromUserToEstate()
                }
            }

            addSource(viewModel.estate) { estate ->
                estateLocation = estate.run { LocationModel(latitude = latitude, longitude = longitude) }
                onSourceUpdated()
            }
            addSource(locationViewModel.lastUserLocation) { location ->
                userLocation = location
                onSourceUpdated()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().getInt(ARG_SELECTED_ESTATE_ID, -1).let { estateId ->
            if (estateId == -1) throw IllegalStateException("Expected argument for Estate Id was not received.")

            viewModel.setSelectedEstateId(estateId)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_estate_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapFragment.getMapAsync(callback)

        viewModel.estate.observe(viewLifecycleOwner, Observer { estate ->
            estate.run {
                requireActivity().findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).let { toolbar ->
                    if (image == null) toolbar.setBackgroundResource(R.drawable.house1) //placeholder
                    else toolbar.background = image!!.toDrawable(requireContext().resources)
                }

                tv_price.text = getString(R.string.price_with_usd_currency, price.commaString)
                //placeholder
                tv_count_bed.text = bedroomCount.toString()
                tv_count_bathroom.text = bathroomCount.toString()
                tv_size.text = size.toString()
                tv_description.text = description
            }
        })

        distanceChangedObservable.observe(viewLifecycleOwner, Observer { distanceMeters ->
            Log.d(TAG, "Both source received updates")

            val km = distanceMeters / 1000
            tv_distance_from_me.text = getString(R.string.estate_details_distance_kilometers_from_user, km)
            tv_distance_from_me.isVisible = true
        })

    }

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        googleMap.uiSettings.isZoomControlsEnabled = true;

        (mapFragment as NestedScrollViewMapFragment).setTouchListener {
            scroll_view.requestDisallowInterceptTouchEvent(true);
        }

        viewModel.estate.observe(viewLifecycleOwner, Observer { estate ->
            val latlng = LatLng(estate.latitude, estate.longitude)
            googleMap.addMarker(MarkerOptions().position(latlng))//.title("Marker in Sydney")
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, ON_MAP_READY_ZOOM_LEVEL))
        })
    }

}