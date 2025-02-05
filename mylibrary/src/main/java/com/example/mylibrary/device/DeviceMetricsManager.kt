package me.innerworks.iw_mobile_auth_android.device

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import me.innerworks.iw_mobile_auth_android.device.biometics.BiometricMetricsManager
import me.innerworks.iw_mobile_auth_android.device.device_ids.DeviceIdManager
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class DeviceMetricsManager(context: Context) {

    // Use WeakReference to hold the context to avoid memory leaks
    private val contextRef: WeakReference<Context> = WeakReference(context)


    // Function to get all device metrics and return as JSONObject
    fun getDeviceMetrics(): JSONObject {
        val metrics = JSONObject()
        metrics.put("deviceName", getDeviceName())
        metrics.put("model", getModel())
        metrics.put("manufacturer", getManufacturer())
        metrics.put("device", getDevice())
        metrics.put("board", getBoard())
        metrics.put("hardware", getHardware())
        metrics.put("brand", getBrand())
        metrics.put("hardwareSerial", getHardwareSerial())
        metrics.put("buildFingerprint", getBuildFingerprint())
        metrics.put("isEmulator", isRunningOnEmulator())
        metrics.put("isDebuggerConnected", isDebuggerConnected())
        contextRef.get()?.let { context -> // Metrics that require context
            metrics.put("isBuildInDebug", isDebuggable(context))
            metrics.put("isFileIntegrityOk", isFileIntegrityOk(context))
            metrics.put("usbDebuggingStatus", getUsbDebuggingStatus(context))
            metrics.put("isClonedApp", isClonedApp(context))
            DeviceIdManager(context).getAllDeviceIds {
                val keysFirst = it.keys.iterator()
                while (keysFirst.hasNext()) {
                    val key = keysFirst.next()
                    metrics.put(key, it[key])
                }
            }

            BiometricMetricsManager(context).getBiometricAuthDetails{
                val keysSecond = it.keys.iterator()
                while (keysSecond.hasNext()) {
                    val key = keysSecond.next()
                    metrics.put(key, it[key])
                }
            }
        }

        return metrics
    }

    // Private helper functions to retrieve metrics
    private fun getDeviceName(): String {
        return Build.DEVICE
    }

    private fun getModel(): String {
        return Build.MODEL
    }

    private fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    private fun getDevice(): String {
        return Build.DEVICE
    }

    private fun getBoard(): String {
        return Build.BOARD
    }

    private fun getHardware(): String {
        return Build.HARDWARE
    }

    private fun getBrand(): String {
        return Build.BRAND
    }

    private fun getHardwareSerial(): String? {
        return Build.SERIAL
    }

    private fun getBuildFingerprint(): String {
        return Build.FINGERPRINT
    }

    private fun getUsbDebuggingStatus(context: Context): String {
        // Example code, implement logic to check if USB debugging is on
        return if (android.provider.Settings.Secure.getInt(
                context.contentResolver,
                android.provider.Settings.Global.ADB_ENABLED, 0
            ) == 1
        ) {
            "On"
        } else {
            "Off"
        }
    }

    // Check if the app is debuggable
    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    // Check file integrity in the app's file directory
    private fun isFileIntegrityOk(context: Context): Boolean {
        val filesDir = context.filesDir
        val files = filesDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (!file.exists() || !file.canRead()) {
                    return false
                }
            }
        }
        return true
    }
    // Check if the app is a cloned app
    private fun isClonedApp(context: Context): Boolean {
        val packageName = context.packageName
        return packageName.contains("clone") || packageName.contains("parallel")
    }

    // Check if a debugger is attached
    private fun isDebuggerConnected(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    // Check if the app is running on an emulator
    private fun isRunningOnEmulator(): Boolean {
        var result = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.lowercase().contains("vbox")
                || Build.FINGERPRINT.lowercase().contains("test-keys")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)

        if (result) return true

        result = (Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.BOARD == "goldfish"
                || Build.PRODUCT == "sdk"
                || Build.PRODUCT == "google_sdk"
                || Build.PRODUCT == "sdk_x86"
                || Build.PRODUCT == "sdk_gphone64_arm64"
                || Build.PRODUCT == "vbox86p"
                || Build.PRODUCT == "emulator"
                || Build.PRODUCT == "simulator")

        return result
    }
}