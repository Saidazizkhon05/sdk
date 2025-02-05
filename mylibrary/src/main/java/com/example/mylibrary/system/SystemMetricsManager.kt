package me.innerworks.iw_mobile_auth_android.system

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.text.format.DateUtils
import me.innerworks.iw_mobile_auth_android.system.root_checker.RootCheckManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.util.TimeZone
import java.util.Locale
import java.util.UUID

internal class SystemMetricsManager(context: Context) {

    // Use WeakReference to hold the context to avoid memory leaks
    private val contextRef: WeakReference<Context> = WeakReference(context)

    val mediaDrm:MediaDrm by lazy {
        MediaDrm(UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"))
    }

    // Public function to collect system metrics and return as a JSONObject
    fun getSystemMetrics(): JSONObject {
        val systemMetrics = JSONObject()

        systemMetrics.put("versionName", getVersionName())
        systemMetrics.put("apiLevel", getApiLevel())
        systemMetrics.put("buildNumber", getBuildNumber())
        systemMetrics.put("buildTime", getBuildTime())
        systemMetrics.put("buildID", getBuildID())
        systemMetrics.put("securityPatchLevel", getSecurityPatchLevel())
        systemMetrics.put("baseband", getBaseband())
        systemMetrics.put("language", getLanguage())
        systemMetrics.put("timeZone", getTimeZone())
        systemMetrics.put("systemUptime", getSystemUptime())
        systemMetrics.put("systemAsRoot", getSystemAsRoot())
        systemMetrics.put("seamlessUpdates", getSeamlessUpdates())
        systemMetrics.put("dynamicPartitions", getDynamicPartitions())
        systemMetrics.put("projectTreble", getProjectTreble())
        systemMetrics.put("javaRuntime", getJavaRuntime())
        systemMetrics.put("javaVM", getJavaVM())
        systemMetrics.put("kernelArchitecture", getKernelArchitecture())
        systemMetrics.put("kernelVersion", getKernelVersion())
        systemMetrics.put("opensslVersion", getOpensslVersion())
        systemMetrics.put("drmVendor", getDRMVendor())
        systemMetrics.put("drmVersion", getDRMVersion())
        systemMetrics.put("drmSecurityLevel", getDRMSecurityLevel())
        contextRef.get()?.let { context ->
            systemMetrics.put("rootAccess", getRootAccess(context))
        }
        return systemMetrics
    }

    private fun getVersionName(): String {
        return Build.VERSION.RELEASE // Returns the version name, e.g., "12" or "13"
    }

    private fun getApiLevel(): Int {
        return Build.VERSION.SDK_INT
    }

    private fun getBuildNumber(): String {
        return Build.DISPLAY
    }

    private fun getBuildTime(): String {
        return DateFormat.format("yyyy-MM-dd hh:mm:ss", Build.TIME).toString()
    }

    private fun getBuildID(): String {
        return Build.ID
    }

    private fun getSecurityPatchLevel(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            null
        }
    }

    private fun getBaseband(): String? {
        return try {
            val command = arrayOf("getprop", "gsm.version.baseband")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun getLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contextRef.get()?.resources?.configuration?.locales?.get(0)?.displayName ?: Locale.getDefault().displayName
        } else {
            Locale.getDefault().displayName
        }
    }

    private fun getTimeZone(): String {
        return TimeZone.getDefault().id
    }

    private fun getRootAccess(context: Context): String {
        return if (RootCheckManager(context).isRooted()) "Yes" else "No"
    }

    private fun getSystemUptime(): String {
        val uptime = SystemClock.elapsedRealtime() // Returns time since boot, including deep sleep
        return DateUtils.formatElapsedTime(uptime / 1000)
    }

    private fun getSystemAsRoot(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "Supported" else "Not Supported"
    }

    private fun getSeamlessUpdates(): String {
        // Check for the existence of A/B partition updates
        return if (File("/dev/block/by-name/system_a").exists() && File("/dev/block/by-name/system_b").exists()) {
            "Supported"
        } else {
            "Not Supported"
        }
    }

    private fun getDynamicPartitions(): String? {
        // You can check if dynamic partitions exist by using commands like `getprop`
        return try {
            val process = Runtime.getRuntime().exec("getprop ro.boot.dynamic_partitions")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            if (result == "true") "Supported" else "Not Supported"
        } catch (e: Exception) {
            null
        }
    }

    private fun getProjectTreble(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val process = Runtime.getRuntime().exec("getprop ro.treble.enabled")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val isTrebleEnabled = reader.readLine()
                if (isTrebleEnabled == "true") "Supported" else "Not Supported"
            } catch (e: Exception) {
                null
            }
        } else {
            "Not Supported"
        }
    }

    private fun getJavaRuntime(): String? {
        return System.getProperty("java.runtime.name")
    }

    private fun getJavaVM(): String? {
        return System.getProperty("java.vm.name")
    }

    private fun getKernelArchitecture(): String? {
        return System.getProperty("os.arch")
    }

    private fun getKernelVersion(): String? {
        return try {
            val process = Runtime.getRuntime().exec("uname -r")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun getOpensslVersion(): String? {
        return try {
            val process = Runtime.getRuntime().exec("openssl version")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun getDRMVendor(): String? {
        return mediaDrm.getPropertyString("vendor") ?: null
    }

    private fun getDRMVersion(): String? {
        return mediaDrm.getPropertyString("version") ?: null
    }

    private fun getDRMSecurityLevel(): String? {
        return mediaDrm.getPropertyString("securityLevel") ?: null
    }

}