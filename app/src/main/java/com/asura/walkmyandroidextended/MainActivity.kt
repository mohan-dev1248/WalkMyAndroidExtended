package com.asura.walkmyandroidplaces

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.asura.walkmyandroidextended.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse
import com.google.android.gms.location.places.Places
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), OnTaskCompleted {

    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var mRotateAnime: AnimatorSet

    private lateinit var mLastLocation: Location
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private var mTrackingLocation = false

    private lateinit var mPlaceDetectionClient: PlaceDetectionClient

    private lateinit var mLastPlaceName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mRotateAnime = AnimatorInflater.loadAnimator(
            this,
            R.animator.rotate
        ) as AnimatorSet

        mRotateAnime.setTarget(androidImageView)


        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        addressTextView.text = getString(
            R.string.address_text,
            R.string.loading,
            R.string.loading,
            System.currentTimeMillis()
        )

        getLocation()

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this)

    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this
                , arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                , REQUEST_LOCATION_PERMISSION
            )
        } else {
            Log.i(TAG, "Location Permission is already there")
        }
    }

    private fun startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this
                , arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                , REQUEST_LOCATION_PERMISSION
            )
        } else {
            Log.i(TAG, "Location Permission is already there")

            mTrackingLocation = true

            trackerButtonTextView.text = getString(R.string.press_the_button_to_stop_tracking)
            trackerButton.text = getString(R.string.stop_tracker_button)

            val locationCallback = CLocationCallback(
                this,
                this,
                mTrackingLocation
            )
            mFusedLocationProviderClient.requestLocationUpdates(
                getLocationRequest(),
                locationCallback,
                null
            )
            mRotateAnime.start()
        }
    }

    private fun getLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    private fun stopTrackingLocation() {
        mTrackingLocation = false
        trackerButtonTextView.text = getString(R.string.press_the_button_to_start_tracking)
        trackerButton.text = getString(R.string.start_tracker_button)

        mRotateAnime.end()
    }

    fun toggleTracking(view: View) {
        if (mTrackingLocation) stopTrackingLocation() else startTrackingLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getLocation()
                } else {
                    Toast.makeText(
                        this,
                        R.string.location_permission_denied,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onTaskCompleted(resultPair: Pair<Location?,String>) {
        if (mTrackingLocation) {
            val placesTask = mPlaceDetectionClient.getCurrentPlace(null)
            placesTask.addOnCompleteListener {
                if(it.isSuccessful){
                    val likelyPlaces = it.getResult() as PlaceLikelihoodBufferResponse
                    var maxLikelihood = 0F
                    var currentPlace: Place? = null
                    likelyPlaces.forEach {
                        if(maxLikelihood< it.likelihood){
                            maxLikelihood = it.likelihood
                            currentPlace = it.place
                        }
                    }
                    likelyPlaces.release()
                    if(currentPlace!=null){
                        setAndroidType(currentPlace!!)
                        setAddressText(currentPlace.toString(),resultPair.second)
                    }
                }
                else{
                    setAddressText(R.string.unable_to_get_place,resultPair.second)
                }
            }
            setAddressText(R.string.loading,resultPair.second)
        }
    }

    private fun setAddressText(placeName: String,address: String){
        addressTextView.text = getString(
            R.string.address_text,
            placeName,
            address,
            System.currentTimeMillis()
        )
    }

    private fun setAddressText(placeNameId: Int, addressId: Int){
        setAddressText(resources.getString(placeNameId),resources.getString(addressId))
    }

    private fun setAddressText(placeNameId: Int, address: String){
        setAddressText(resources.getString(placeNameId),address)
    }


    private fun setAndroidType(currentPlace: Place) {
        var drawableID = -1
        for (placeType in currentPlace.placeTypes) {
            when (placeType) {
                Place.TYPE_SCHOOL -> drawableID = R.drawable.android_school
                Place.TYPE_GYM -> drawableID = R.drawable.android_gym
                Place.TYPE_RESTAURANT -> drawableID = R.drawable.android_restaurant
                Place.TYPE_LIBRARY -> drawableID = R.drawable.android_library
            }
        }

        if (drawableID < 0) {
            drawableID = R.drawable.android_plain
        }
        androidImageView.setImageResource(drawableID)
    }
    override fun onPause() {
        if (mTrackingLocation) {
            stopTrackingLocation()
            mTrackingLocation = true
        }
        super.onPause()
    }

    override fun onResume() {
        if (mTrackingLocation) {
            startTrackingLocation()
        }
        super.onResume()
    }
}