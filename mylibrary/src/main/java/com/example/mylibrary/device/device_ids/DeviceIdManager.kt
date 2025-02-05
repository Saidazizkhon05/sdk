package me.innerworks.iw_mobile_auth_android.device.device_ids

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import java.lang.reflect.Method
import java.lang.ref.WeakReference

internal class DeviceIdManager(context: Context) {

    private val contextRef = WeakReference(context)

    // Method to collect all IDs and return as a JSONObject
    fun getAllDeviceIds(callback: (Map<String, String>) -> Unit) {
        val deviceIds = HashMap<String, String>()
        val context = contextRef.get()

        context?.let {
            // Get Android Device ID
            deviceIds["androidDeviceID"] = getAndroidDeviceID(context)

            // Get Application ID
            deviceIds["applicationId"] = getApplicationId(context)

            // Get Google Framework ID (if accessible)
            getGoogleFrameworkID(context)?.let {
                deviceIds["googleFrameworkID"] = it
            }

            // Get Google Advertising ID asynchronously
            getGoogleAdvertisingID(context) { gaid ->
                deviceIds.put("googleAdvertisingID", gaid ?: "Unavailable")
                // Return the collected IDs via callback
                callback(deviceIds)
            }
        } ?: run {
            deviceIds["IdCollectionError"] = "Context not available"
            callback(deviceIds)
        }
    }

    // Method to retrieve the Android Device ID
    @SuppressLint("HardwareIds")
    private fun getAndroidDeviceID(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // Method to retrieve the Google Framework ID (GSF ID)
    private fun getGoogleFrameworkID(context: Context): String? {
        return try {
            val uri = android.net.Uri.parse("content://com.google.android.gsf.gservices")
            val selectionArgs = arrayOf("android_id")
            val cursor = context.contentResolver.query(uri, null, null, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst() && cursor.columnCount >= 2) {
                val gsfId = cursor.getString(1)
                cursor.close()
                gsfId
            } else {
                cursor?.close()
                null
            }
        } catch (e: Exception) {
            Log.e("DeviceIdManager", "Error retrieving Google Framework ID: ${e.message}")
            null
        }
    }

    // Method to retrieve Google Advertising ID using reflection
    private fun getGoogleAdvertisingID(context: Context, callback: (String?) -> Unit) {
        try {
            // Load AdvertisingIdClient class dynamically via reflection
            val advertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            val getAdvertisingIdInfoMethod = advertisingIdClientClass.getMethod("getAdvertisingIdInfo", Context::class.java)

            // Call the method and get the adInfo object
            val adInfo = getAdvertisingIdInfoMethod.invoke(null, context)
            val adInfoClass = adInfo.javaClass

            // Retrieve the advertising ID from adInfo object
            val getIdMethod: Method = adInfoClass.getMethod("getId")
            val adId = getIdMethod.invoke(adInfo) as String

            callback(adId)
        } catch (e: ClassNotFoundException) {
            Log.e("DeviceIdManager", "Google Play Services not available, cannot retrieve GAID.")
            callback(null)
        } catch (e: Exception) {
            Log.e("DeviceIdManager", "Error retrieving Google Advertising ID: ${e.message}")
            callback(null)
        }
    }

    // Method to retrieve the Application ID of the parent app that integrates the SDK
    private fun getApplicationId(context: Context): String {
        return context.packageName
    }
}