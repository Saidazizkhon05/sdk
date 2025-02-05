package me.innerworks.iw_mobile_auth_android.storage

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

internal class StorageMetricsManager(context: Context) {

    private val contextRef: WeakReference<Context> = WeakReference(context)

    // Public method to get all storage metrics (RAM, Internal, System, etc.)
    fun getStorageMetrics(): JSONObject {
        val storageMetrics = JSONObject()
        val context = contextRef.get() ?: return errorResult("Context is not available")

        // Get RAM metrics
        storageMetrics.put("RAM", getRamMetrics(context))

        // Get Internal Storage metrics
        storageMetrics.put("Internal Storage", getInternalStorageMetrics())

        // Get System Storage metrics (Root storage typically /system)
        storageMetrics.put("System Storage", getSystemStorageMetrics())

        return storageMetrics
    }

    // Method to get RAM metrics
    private fun getRamMetrics(context: Context): JSONObject {
        val ramMetrics = JSONObject()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem.toDouble() / (1024 * 1024) // Convert to MB
        val availableRam = memoryInfo.availMem.toDouble() / (1024 * 1024) // Convert to MB
        val usedRam = totalRam - availableRam
        val ramUsagePercent = ((usedRam / totalRam) * 100).toInt()

        ramMetrics.put("totalRAM", String.format("%.1f MB", totalRam))
        ramMetrics.put("freeRAM", String.format("%.1f MB", availableRam))
        ramMetrics.put("usedRAM", String.format("%.1f MB", usedRam))
        ramMetrics.put("usagePercent", "$ramUsagePercent%")

        return ramMetrics
    }

    // Method to get Internal Storage metrics (typically /storage/emulated/0)
    private fun getInternalStorageMetrics(): JSONObject {
        val internalStorageMetrics = JSONObject()

        val internalStorageDir = Environment.getExternalStorageDirectory()
        val stat = StatFs(internalStorageDir.path)

        val totalStorage = stat.totalBytes.toDouble() / (1024 * 1024 * 1024) // Convert to GB
        val freeStorage = stat.availableBytes.toDouble() / (1024 * 1024 * 1024) // Convert to GB
        val usedStorage = totalStorage - freeStorage
        val storageUsagePercent = ((usedStorage / totalStorage) * 100).toInt()

        internalStorageMetrics.put("totalStorage", String.format("%.1f GB", totalStorage))
        internalStorageMetrics.put("freeStorage", String.format("%.1f GB", freeStorage))
        internalStorageMetrics.put("usedStorage", String.format("%.1f GB", usedStorage))
        internalStorageMetrics.put("usagePercent", "$storageUsagePercent%")

        return internalStorageMetrics
    }

    // Method to get System Storage metrics (typically /system)
    private fun getSystemStorageMetrics(): JSONObject {
        val systemStorageMetrics = JSONObject()

        val systemStorageDir = File("/system")
        val stat = StatFs(systemStorageDir.path)

        val totalStorage = stat.totalBytes.toDouble() / (1024 * 1024 * 1024) // Convert to GB
        val freeStorage = stat.availableBytes.toDouble() / (1024 * 1024 * 1024) // Convert to GB
        val usedStorage = totalStorage - freeStorage
        val storageUsagePercent = ((usedStorage / totalStorage) * 100).toInt()

        systemStorageMetrics.put("totalStorage", String.format("%.1f GB", totalStorage))
        systemStorageMetrics.put("freeStorage", String.format("%.1f GB", freeStorage))
        systemStorageMetrics.put("usedStorage", String.format("%.1f GB", usedStorage))
        systemStorageMetrics.put("usagePercent", "$storageUsagePercent%")

        return systemStorageMetrics
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}