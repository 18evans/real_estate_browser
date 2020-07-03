package evans18.realestatebrowser.ui.fragment.estate

import android.app.Application
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import evans18.realestatebrowser.data.model.local.LocationModel
import evans18.realestatebrowser.util.TAG

/**
 * ViewModel responsible for handling Location Requests in a lifecycle scope.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val REQUEST_CHECK_SETTINGS = 0

        private val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        private val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
    }

    /**
     * Tracks the status of the location updates request.
     * Value changes when the [FusedLocationProviderClient] (un)registers a [LocationRequest].
     *
     * Note: Only used so as to not by off-chance set duplicate [FusedLocationProviderClient.requestLocationUpdates] requests
     * to the [FusedLocationProviderClient] internal structure.
     * Docs say that duplication is handled by replacement which is still more expensive operation than a simple boolean flag check.
     */
    private var isCurrentlyRequestingLocationUpdates = false
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(application)

    private val _locationData = MutableLiveData<LocationModel>()

    /**
     * Subscribe to this to receive [LocationModel] data.
     */
    val lastUserLocation: LiveData<LocationModel> = _locationData

    private val _gpsExceptionObservable = MutableLiveData<Event<Exception>>()

    /**
     * Propagates exceptions to the View whenever the [SettingsClient.checkLocationSettings] fails (ie. confirms that GPS is disabled)
     * [ResolvableApiException] is its response which is propagated to th view to prompt the GpsRequest dialog to the user with Enable/Disable answers.
     */
    val gpsExceptionObservable: LiveData<Event<Exception>> = _gpsExceptionObservable

    /**
     * Location data is received here.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations) {
                _locationData.value = LocationModel(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
        }
    }

    /**
     * Requests location updates only if GPS is enabled.
     * If not, requests gps enable.
     *
     * Note: Expects location permissions to be granted.
     */
    @RequiresPermission(anyOf = [android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION])
    fun startLocationUpdates() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnFailureListener { e ->
                _gpsExceptionObservable.postValue(Event(e))
            }
            .addOnSuccessListener {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
//                    Looper.myLooper() //todo necessary?
                )
                isCurrentlyRequestingLocationUpdates = true;
            }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     *
     * It is a good practice to remove location requests when the activity is in a paused or
     * stopped state. Doing so helps battery performance and is especially
     * recommended in applications that request frequent location updates.
     */
    fun stopLocationUpdates() {
        if (!isCurrentlyRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates were never requested")
            return
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener {
                isCurrentlyRequestingLocationUpdates = false
            }
    }

}
