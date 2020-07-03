package evans18.realestatebrowser.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.material.bottomnavigation.BottomNavigationView
import evans18.realestatebrowser.BuildConfig
import evans18.realestatebrowser.R
import evans18.realestatebrowser.ui.fragment.estate.LocationViewModel
import evans18.realestatebrowser.ui.fragment.estate.LocationViewModel.Companion.REQUEST_CHECK_SETTINGS
import evans18.realestatebrowser.util.TAG
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    companion object {
        const val LOCATION_PERMISSION_DID_NEVER_ASK_AGAIN = "LOCATION_PERMISSION_DID_NEVER_ASK_AGAIN"
    }

    /**
     * Keeps track for the current lifecycle of this Activity.
     * whether permission was denied in order not to spam user until he exits these lifecycle steps.
     */
    private var isAcceptableToAskLocationPermission = true

    /**
     * Keeps track for the current lifecycle of this Activity.
     * whether the GPS request prompt was denied in order not to spam user until he exits these lifecycle steps.
     */
    private var isAcceptableToRequestGps = true

    private val locationViewModel: LocationViewModel by viewModels()

    private val navController by lazy {
        findNavController(R.id.nav_host_fragment).apply {
            addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.navigation_estate_detail) {
                    toolbar_layout.title = "" //for some reason this is the only way to make it take the XML navigation's "title" attribute value
                    app_bar.setExpanded(true)
                } else {
                    toolbar_layout.background = null
                    toolbar_layout.title = destination.label
                    if (destination.id == R.id.navigation_information) {
                        app_bar.setExpanded(true)
                    }

                }
            }
        }
    }
    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(R.id.navigation_estates, R.id.navigation_information)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar)) //necessary in order to initiate the androidx toolbar into the theme so that `setupActionBarWithNavController` doesn't throw NPE

        setupActionBarWithNavController(navController, appBarConfiguration)
        findViewById<BottomNavigationView>(R.id.nav_view).setupWithNavController(navController)

        bindObservables()
    }

    private fun bindObservables() {
        locationViewModel.apply {
            gpsExceptionObservable.observe(this@MainActivity, Observer { event ->
                event.getContentIfNotHandled()?.let { ex -> // Only proceed if the event has not been handled
                    (ex as ApiException).statusCode.run {
                        when (this) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                                if (!isAcceptableToRequestGps)
                                    return@run //don't ask if NOT lifecycle-polite
                                else
                                    try {
                                        // Show the dialog by calling startResolutionForResult(), and check the
                                        // result in onActivityResult().
                                        val rae = ex as ResolvableApiException
                                        rae.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                                    } catch (sie: IntentSender.SendIntentException) {
                                        Log.i(ContentValues.TAG, "PendingIntent unable to execute request.")
                                    } //not acceptable to ask any more
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                val errorMessage = getString(R.string.gps_settings_inadequate)
                                Log.e(ContentValues.TAG, errorMessage)
                                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
//                                locationViewModel.setGpsEnableRequestResponse(false)
                            }
                            else -> Log.e(TAG, "Unrecognized status code.")
                        }
                    }
                }
            })
        }

    }

    override fun onStart() {
        super.onStart()
        if (isAcceptableToAskLocationPermission) //only if lifecycle-polite
            startLocationUpdatesWithPermissionCheck() //check permissions
    }

    override fun onStop() {
        super.onStop()
        // Remove location updates to save battery.
        locationViewModel.stopLocationUpdates()
    }

    /**
     * Supports back navigation after navigation actions.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                when (resultCode) {
                    Activity.RESULT_CANCELED -> {
                        Log.i(TAG, "User chose not to make required location settings changes.")
                        isAcceptableToRequestGps = false
                    }
                    Activity.RESULT_OK -> {
                        Log.i(
                            TAG,
                            "User agreed to make required location settings changes."
                        )
                        startLocationUpdatesWithPermissionCheck()
                    }
                }
            }
        }
    }

    //Methods - Permission Handling

    /**
     * Warning:
     * Instead call build-time generated [startLocationUpdatesWithPermissionCheck].
     *
     * This method will only ever be called after [startLocationUpdatesWithPermissionCheck]
     * if the permissions in the annotation have been granted.
     */
    @SuppressLint("MissingPermission")
    @UnsafeDirectCall(
        "Call the generated function with suffix \"WithPermissionCheck\" or only ever call this when you're" +
                "sure permission is given. "
    )
    @NeedsPermission(value = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun startLocationUpdates() {
        locationViewModel.startLocationUpdates()
    }

    @OnShowRationale(value = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun showRationaleForLocation(request: PermissionRequest) {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_rationale_location_title)
            .setMessage(R.string.permission_rationale_location)
            .setPositiveButton("OK") { _, which ->
                when (which) {
                    BUTTON_POSITIVE -> request.proceed() //no need to cancel
                }
            }
            .show()
    }

    @OnPermissionDenied(value = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun onLocationPermissionDenied() {
        Toast.makeText(this, R.string.permission_denied_location, Toast.LENGTH_SHORT).show();
        this.isAcceptableToAskLocationPermission = false
    }

    /**
     * Called when user selects "Deny & Don't ask again" and every time afterwards
     * that the same permissions are requested.

     * Using a shared preference value the notification Toast message is displayed only once.
     */
    @OnNeverAskAgain(value = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun onLocationNeverAskAgain() {
        //todo ideally move the preference handling to a separate class
        val prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(LOCATION_PERMISSION_DID_NEVER_ASK_AGAIN, false)) {
            Toast.makeText(this, R.string.permission_camera_never_askagain, Toast.LENGTH_SHORT).show()
            val editor = prefs.edit()
            editor.putBoolean(LOCATION_PERMISSION_DID_NEVER_ASK_AGAIN, true)
            editor.apply()
        }
        this.isAcceptableToAskLocationPermission = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.onRequestPermissionsResult(requestCode, grantResults)
    }
}

@Target(AnnotationTarget.FUNCTION)
private annotation class UnsafeDirectCall(val reason: String)