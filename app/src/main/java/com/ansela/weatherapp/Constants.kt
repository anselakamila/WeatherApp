package com.ansela.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val API_KEY: String = "2b25ca3ae3701f9f6ac0aae36bde58fd"
    const val METRIC_UNIT: String = "metric"


    fun isNetworkAvailable(context: Context): Boolean {
        //membuat object baru yang akan memberi kita layanan konekvitas
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as
                ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activityNetwork =
                connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting

        }

    }
}