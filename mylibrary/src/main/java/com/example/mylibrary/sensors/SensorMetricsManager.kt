package me.innerworks.iw_mobile_auth_android.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class SensorMetricsManager(context: Context) {

    private val contextRef = WeakReference(context)

    // Public method to get all sensor metrics as a formatted JSONObject
    fun getFormattedSensorMetrics(): JSONObject {
        val sensorMetrics = JSONObject()
        val context = contextRef.get() ?: return errorResult("Context is not available")

        try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)

            // Iterate over the sensor list and add each sensor to the JSON object
            for (sensor in sensorList) {
                val sensorName = getSensorType(sensor.type)
                val sensorInfo = getSensorInfo(sensor)
                sensorMetrics.put(sensorName, sensorInfo)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sensorMetrics
    }

    // Private method to get detailed information for each sensor
    private fun getSensorInfo(sensor: Sensor): JSONObject {
        val sensorInfo = JSONObject()

        sensorInfo.put("name", sensor.name)
        sensorInfo.put("vendor", sensor.vendor)
        sensorInfo.put("power", "${sensor.power} mA")
        sensorInfo.put("wakeUpSensor", if (sensor.isWakeUpSensor) "No" else "Yes")

        return sensorInfo
    }

    // Method to return sensor type as a readable name
    private fun getSensorType(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
            Sensor.TYPE_ORIENTATION -> "Orientation"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PRESSURE -> "Pressure"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector"
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic Rotation Vector"
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Magnetic Field Uncalibrated"
            else -> "Unknown Sensor"
        }
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}