package me.innerworks.iw_mobile_auth_android.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class LocationMetricsManager(context: Context) {

    // Use WeakReference to hold the context to avoid memory leaks
    private val contextRef: WeakReference<Context> = WeakReference(context)

    // Public method to get all location and GNSS metrics as a JSONObject
    fun getLocationMetrics(): JSONObject {
        val locationMetrics = JSONObject()

        val context = contextRef.get() ?: return errorResult("Context is not available")

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Retrieve location metrics
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            val location = getLastKnownLocation(locationManager)

            if (location != null) {
                // Basic location metrics
                locationMetrics.put("latitude", location.latitude)
                locationMetrics.put("longitude", location.longitude)
                locationMetrics.put("altitude", location.altitude)
                locationMetrics.put("speed", location.speed)
                locationMetrics.put("bearing", location.bearing)
                locationMetrics.put("accuracy", location.accuracy)

                // Extended metrics
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    locationMetrics.put("bearingAccuracy", location.bearingAccuracyDegrees)
                    locationMetrics.put("speedAccuracy", location.speedAccuracyMetersPerSecond)
                    locationMetrics.put("verticalAccuracy", location.verticalAccuracyMeters)
                }

                // Add GNSS status (satellites) metrics
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locationMetrics.put("gnssMetrics", getGnssMetrics(locationManager))
                }
            } else {
                return errorResult("Location could not be retrieved.")
            }
        } else {
            return errorResult("Location permissions are not granted.")
        }

        return locationMetrics
    }

    // Private method to retrieve the last known location
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(locationManager: LocationManager?): Location? {
        if (locationManager == null) return null
        var location: Location? = null
        try {
            locationManager.getProviders(true).forEach { provider ->
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null && (location == null || loc.accuracy < (location?.accuracy ?: 0f))) {
                    location = loc
                }
            }
        } catch (e: SecurityException) {
            return null
        } catch (e: Exception) {
            return null
        }

        return location
    }

    // Method to get GNSS metrics such as satellite information and status
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getGnssMetrics(locationManager: LocationManager?): JSONObject {
        val gnssMetrics = JSONObject()

        if (locationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return errorResult("GNSS data not available")
        }

        try {
            locationManager.registerGnssStatusCallback(object : GnssStatus.Callback(){}, Handler())
            locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    gnssMetrics.put("satelliteCount", status.satelliteCount)

                    val satellites = JSONObject()
                    for (i in 0 until status.satelliteCount) {
                        val satelliteData = JSONObject()
                        satelliteData.put("constellation", getConstellationName(status.getConstellationType(i)))
                        satelliteData.put("azimuthDegrees", status.getAzimuthDegrees(i))
                        satelliteData.put("elevationDegrees", status.getElevationDegrees(i))
                        satelliteData.put("snr", status.getCn0DbHz(i))
                        satelliteData.put("usedInFix", status.usedInFix(i))

                        satellites.put("satellite_$i", satelliteData)
                    }
                    gnssMetrics.put("satellites", satellites)
                }
            }, Handler())
        } catch (e: SecurityException) {
            return errorResult("Failed to retrieve GNSS data")
        }

        return gnssMetrics
    }

    // Private helper method to get constellation names for GNSS systems (GPS, GLONASS, etc.)
    private fun getConstellationName(constellationType: Int): String {
        return when (constellationType) {
            GnssStatus.CONSTELLATION_GPS -> "GPS"
            GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
            GnssStatus.CONSTELLATION_BEIDOU -> "Beidou"
            GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
            GnssStatus.CONSTELLATION_QZSS -> "QZSS"
            GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
            else -> "Unknown"
        }
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}