package me.innerworks.iw_mobile_auth_android.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class BatteryInfoManager(context: Context) {

    private val contextRef = WeakReference(context)

    fun getBatteryInfo(): JSONObject {
        val batteryInfo = JSONObject()
        val context = contextRef.get() ?: return batteryInfo

        try {
            // Get the BatteryManager
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            // Register for battery status changes
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)

            batteryStatus?.let {
                // Basic battery level and charging information
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val isPluggedIn = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0
                val batteryPct = (level / scale.toFloat()) * 100

                batteryInfo.put("level", batteryPct.toInt())
                batteryInfo.put("isCharging", isCharging)
                batteryInfo.put("isPluggedIn", isPluggedIn)

                // Additional static battery information
                val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val technology = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

                // Mapping health status
                val healthStatus = getBatteryHealth(health)

                // Estimated battery capacity (mAh) - only available on newer Android versions
                val batteryCapacity = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                batteryInfo.put("capacity_mAh", batteryCapacity)

                // Putting additional battery information into the JSON object
                batteryInfo.put("health", healthStatus)
                batteryInfo.put("voltage", voltage) // in mV
                batteryInfo.put("temperature", temperature / 10.0) // convert from deci-Celsius to Celsius
                batteryInfo.put("technology", technology)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return batteryInfo
    }

    // Helper method to map battery health status
    private fun getBatteryHealth(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
}