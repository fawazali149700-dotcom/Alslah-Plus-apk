package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val cityNameAr: String,
    val countryNameAr: String = ""
)

class LocationHelper(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): Result<ResolvedLocation> = withContext(Dispatchers.IO) {
        try {
            val location = getCurrentOrLastLocation()
                ?: return@withContext Result.failure(IllegalStateException("تعذر الحصول على الموقع الحالي"))
            val (city, country) = resolveCityAndCountry(location.latitude, location.longitude)
            Result.success(ResolvedLocation(location.latitude, location.longitude, city, country))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCityByName(query: String): List<ResolvedLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val geocoder = Geocoder(context, Locale("ar"))
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 6) { addresses ->
                        val mapped = addresses.mapNotNull { addr ->
                            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                            if (city.isNullOrBlank()) null
                            else ResolvedLocation(
                                latitude = addr.latitude,
                                longitude = addr.longitude,
                                cityNameAr = city,
                                countryNameAr = addr.countryName ?: ""
                            )
                        }
                        if (cont.isActive) cont.resume(mapped)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 6).orEmpty()
                addresses.mapNotNull { addr ->
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                    if (city.isNullOrBlank()) null
                    else ResolvedLocation(
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        cityNameAr = city,
                        countryNameAr = addr.countryName ?: ""
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentOrLastLocation(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (cont.isActive) cont.resume(loc)
                    } else {
                        fusedClient.lastLocation
                            .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    fusedClient.lastLocation
                        .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
        }

    private suspend fun resolveCityAndCountry(lat: Double, lon: Double): Pair<String, String> {
        val geocoder = Geocoder(context, Locale("ar"))
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        val city = addr?.locality ?: addr?.adminArea ?: addr?.countryName
                            ?: String.format(Locale.US, "%.2f, %.2f", lat, lon)
                        val country = addr?.countryName ?: ""
                        if (cont.isActive) cont.resume(city to country)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                val city = addr?.locality ?: addr?.adminArea ?: addr?.countryName
                    ?: String.format(Locale.US, "%.2f, %.2f", lat, lon)
                val country = addr?.countryName ?: ""
                city to country
            }
        } catch (_: Exception) {
            String.format(Locale.US, "%.2f, %.2f", lat, lon) to ""
        }
    }
}
