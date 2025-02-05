package me.innerworks.iw_mobile_auth_android

import android.content.Context
import android.view.MotionEvent
import me.innerworks.iw_mobile_auth_android.apps.AppsMetricsManager
import me.innerworks.iw_mobile_auth_android.battery.BatteryInfoManager
import me.innerworks.iw_mobile_auth_android.camera.CameraMetricsManager
import me.innerworks.iw_mobile_auth_android.cpu.CPUMetricsManager
import me.innerworks.iw_mobile_auth_android.device.DeviceMetricsManager
import me.innerworks.iw_mobile_auth_android.display.DisplayMetricsManager
import me.innerworks.iw_mobile_auth_android.location.LocationMetricsManager
import me.innerworks.iw_mobile_auth_android.network.NetworkMetricsManager
import me.innerworks.iw_mobile_auth_android.send_collected_data.SendCollectedMetrics
import me.innerworks.iw_mobile_auth_android.sensors.SensorMetricsManager
import me.innerworks.iw_mobile_auth_android.storage.StorageMetricsManager
import me.innerworks.iw_mobile_auth_android.system.SystemMetricsManager
import me.innerworks.iw_mobile_auth_android.user_metrics.UserMetricsManager
import me.innerworks.iw_mobile_auth_android.utils.extentions.format
import org.json.JSONObject
import java.util.Date


internal class InnerworksMetricsManager(val context: Context): InnerworksMetrics{

    private val userMetricsManager = UserMetricsManager()
    private val sendCollectedMetrics = SendCollectedMetrics(context)
    private var startSession: Date = Date()
    private var allMetrics = JSONObject()

    // Public method to collect all metrics from different managers into one payload
    private fun collectAllMetrics(): JSONObject {
        try {
            // Device Metrics
            val deviceMetricsManager = DeviceMetricsManager(context)
            allMetrics.put("deviceMetrics", deviceMetricsManager.getDeviceMetrics())

            //CPU metrics
            val cpuMetricsManager = CPUMetricsManager(context)
            allMetrics.put("cpuMetrics", cpuMetricsManager.getCPUMetrics())

            //System metrics
            val systemMetricsManager = SystemMetricsManager(context)
            allMetrics.put("systemMetrics", systemMetricsManager.getSystemMetrics())

            // Network Metrics
            val networkMetricsManager = NetworkMetricsManager(context)
            allMetrics.put("networkMetrics", networkMetricsManager.getNetworkMetrics())

            // Battery Metrics
            val batteryInfoManager = BatteryInfoManager(context)
            allMetrics.put("batteryMetrics", batteryInfoManager.getBatteryInfo())

            // Storage Metrics
            val storageMetricsManager = StorageMetricsManager(context)
            allMetrics.put("storageMetrics", storageMetricsManager.getStorageMetrics())

            // Display Metrics
            val displayMetricsManager = DisplayMetricsManager(context)
            allMetrics.put("displayMetrics", displayMetricsManager.getDisplayMetrics())

            // Camera Metrics
            val cameraMetricsManager = CameraMetricsManager(context)
            allMetrics.put("cameraMetrics", cameraMetricsManager.getCameraMetrics())

            // Sensor Metrics
            val sensorMetricsManager = SensorMetricsManager(context)
            allMetrics.put("sensorMetrics", sensorMetricsManager.getFormattedSensorMetrics())

            // App Info Metrics
            val appInfoManager = AppsMetricsManager(context)
            allMetrics.put("appMetrics", appInfoManager.getCategorizedApps())

            val locationMetricsManager = LocationMetricsManager(context)
            allMetrics.put("locationMetrics", locationMetricsManager.getLocationMetrics())

            // User Metrics
            allMetrics.put("userMetrics", userMetricsManager.getUserMetrics())

            //Session Metrics
            allMetrics.put("session_time", "${Date().format()}~${startSession.format()}")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return allMetrics
    }

    override fun sendCollectedData(data: JSONObject, onFail: (msg: String)->Unit, onSuccess: ()->Unit) {
        val payload = collectAllMetrics()
        sendCollectedMetrics.sendPostRequest(
            data
                .put("metrics", payload)
                .put("sdk_type", "Android"),
            onFail,
            onSuccess
        )
    }

    override fun textOnChange(typing: String) {
        userMetricsManager.textOnChange(typing)
    }

    override fun sendTouchEvent(event: MotionEvent) {
        userMetricsManager.handleTouchEvent(event)
    }

    override fun setBaseUrl(url: String) {
        sendCollectedMetrics.setBaseUrl(url)
    }

    override fun setOptionalData(data: JSONObject) {
        allMetrics.put("optional_data", data)
    }
}