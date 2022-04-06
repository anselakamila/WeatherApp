package com.ansela.weatherapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import com.ansela.weatherapp.Constants.API_KEY
import com.ansela.weatherapp.Constants.METRIC_UNIT
import com.ansela.weatherapp.models.WeatherResponse
import com.ansela.weatherapp.network.WeatherConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your Location provider is turned off.Please turn it on",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION

                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission." +
                                        "Please Enable Them as it Mandatory for the app to work",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogPermission()
                    }
                }).onSameThread()
                .check()

            Toast.makeText(
                this,
                "Your Location Provider is already on.",
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    private fun showRationalDialogPermission() {
        AlertDialog.Builder(this)
            .setMessage(
                "It Look Like you have turned off permissions required for this feature." +
                        "It can be enabled under Application Settings"
            )
            .setPositiveButton(
                "Go To Settings"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,

                                           _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Longitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            //tambahkan ini
            getLocationWeatherDetails(latitude, longitude)
        }

    }

    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){
        if (Constants.isNetworkAvailable(this)) {
            val client = WeatherConfig.getWeatherService()
                .getWeather(latitude, longitude, METRIC_UNIT, API_KEY)

            showCustomProgressDialog()

            client.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null){

                        hideProgressDialog()
                        //tambahkan juga
                        setupUI(responseBody)
                        Log.i("Response Result", "$responseBody")
                    }else{
                        Log.e(TAG, "onFailure: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG,"Error: ${t.message.toString()}")
                    hideProgressDialog()
                }
            })
        } else{
            Toast.makeText(
                this@MainActivity,
                "No Internet Connection Available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog(){
        if (mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }
    }

    private fun setupUI(responsebody:WeatherResponse){
        for (i in responsebody.weather.indices){
            Log.i("Weather Name", responsebody.weather.toString())

            val tvCity: TextView = findViewById(R.id.tv_city_code)
            tvCity.text = responsebody.name
            val tvStatus: TextView = findViewById(R.id.tv_status)
            tvStatus.text = responsebody.weather[i].description
            val tvDegree: TextView = findViewById(R.id.tv_degree)
            tvDegree.text = responsebody.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            val tvMinTemp: TextView = findViewById(R.id.tv_max_temp)
            tvMinTemp.text = responsebody.main.temp_min.toString() + "main"
            val tvMaxTemp: TextView = findViewById(R.id.tv_max_temp)
            tvMaxTemp.text = responsebody.main.temp_max.toString() + "max"

            val tvSunriseTime: TextView = findViewById(R.id.tv_sunrise)
            tvSunriseTime.text = unixTime(responsebody.sys.sunrise)
            val tvSunsetTime: TextView = findViewById(R.id.tv_sunset)
            tvSunsetTime.text = unixTime(responsebody.sys.sunrise)

            val tvWind: TextView = findViewById(R.id.tv_wind)
            tvWind.text = responsebody.wind.speed.toString() + "miles/hour"

            val tvPressure: TextView = findViewById(R.id.tv_pressure)
            tvPressure.text = responsebody.main.pressure.toString()

            val tvHumidity: TextView = findViewById(R.id.tv_humidity)
            tvHumidity.text = responsebody.main.humidity.toString() + "per cent"

            val tvVisibility: TextView = findViewById(R.id.tv_visibility)
            tvVisibility.text = responsebody.visibility.toString()

            val ivWeather: ImageView = findViewById(R.id.iv_weather)

            when(responsebody.weather[i].icon){
                "01d" -> ivWeather.setImageResource(R.drawable.ic_sunny)
                "02d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "03d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "04d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "09d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "10d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "11d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "13d" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "01n" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "02n" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "03n" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "10n" -> ivWeather.setImageResource(R.drawable.ic_cloud)
                "11n" -> ivWeather.setImageResource(R.drawable.ic_cloud)












            }

            
        }
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)

    }

    private fun getUnit(value: String): String? {
        var value = " °C"
        if ("US" == value || "LR" == value || "MM" == value){
            value = " °F"
        }
        return value

    }


}