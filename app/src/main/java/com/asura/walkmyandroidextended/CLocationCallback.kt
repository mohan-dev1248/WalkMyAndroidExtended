package com.asura.walkmyandroidplaces

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

class CLocationCallback(
    private val ctx: Context,
    private val onTaskCompleted: OnTaskCompleted,
    private val trackingLocation: Boolean
): LocationCallback() {

    override fun onLocationResult(locationResult: LocationResult?) {
        Log.i("CLocation","Getting the call")
        if(trackingLocation){
            Log.i("CLocation","inside the if block")
            FetchAddressTask(onTaskCompleted).execute(locationResult!!.lastLocation)
        }
    }
}