package sample.android.location

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import java.util.concurrent.TimeUnit

class SwitchFragment : Fragment() {
    private fun checkLocationAvailable() {
        val act = activity ?: return
        val checkRequest = LocationSettingsRequest.Builder().addLocationRequest(
            getLocationRequest()).build()
        val checkTask = LocationServices.getSettingsClient(act).checkLocationSettings(checkRequest)
        checkTask.addOnCompleteListener { response ->
            try {
                response.getResult(ApiException::class.java)
                checkLocationPermission()
            } catch (exception: ApiException) {
                if (exception.statusCode ==
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    val resolvable = exception as ResolvableApiException
                    resolvable.startResolutionForResult(activity, 1)
                } else {
                    showErrorMessage()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            checkLocationPermission()
        } else {
            showErrorMessage()
        }
    }
    private fun checkLocationPermission() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(ctx, LocationService::class.java)
            val service = PendingIntent.getService(ctx, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
            LocationServices.getFusedLocationProviderClient(ctx)
                .requestLocationUpdates(getLocationRequest(), service)
            ctx.getSharedPreferences("LocationRequesting",
                Context.MODE_PRIVATE)
                .edit().putBoolean("isRequesting", true).apply()
        } else {
            requestPermissions(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION),1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (permissions.isEmpty() && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission()
        } else {
            showErrorMessage()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_switch, container, false)
        val switch = view.findViewById<Switch>(R.id.locationSwitch)
        val isRequesting = context?.getSharedPreferences(
            "LocationRequesting", Context.MODE_PRIVATE)
            ?.getBoolean("isRequesting", false) ?: false
        switch.isChecked = isRequesting
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkLocationAvailable()
            } else {
                stopLocationRequest()
            }
        }
        return view
    }

    private  fun stopLocationRequest() {
        val ctx = context ?: return
        val intent = Intent(ctx, LocationService::class.java)
        val service = PendingIntent.getService(ctx, 1,
            intent, PendingIntent.FLAG_UPDATE_CURRENT)
        LocationServices.getFusedLocationProviderClient(ctx)
            .removeLocationUpdates(service)
        ctx.getSharedPreferences("LocationRequesting", Context.MODE_PRIVATE)
            .edit().putBoolean("isRequesting", true).apply()
    }
    private fun showErrorMessage() {
        Toast.makeText(context, "位置情報を取得することができません",
            Toast.LENGTH_SHORT).show()
        activity?.finish()
    }
    private fun getLocationRequest() =
            LocationRequest()
                .setInterval(TimeUnit.MINUTES.toMillis(5))
                .setFastestInterval(TimeUnit.MINUTES.toMillis(1))
                .setMaxWaitTime(TimeUnit.MINUTES.toMillis(20))
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
}