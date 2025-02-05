package me.innerworks.iw_mobile_auth_android.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class AppsMetricsManager(context: Context) {

    private val contextRef = WeakReference(context)

    // Public method to get the categorized app list as a JSONObject
    fun getCategorizedApps(): JSONObject {
        val appMetrics = JSONObject()
        val context = contextRef.get() ?: return errorResult("Context is not available")

        try {
            val packageManager = context.packageManager
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val userAppsArray = JSONArray()
            val systemAppsArray = JSONArray()

            for (app in installedApps) {
                val appInfo = getAppInfo(app, packageManager)
                if (isSystemApp(app)) {
                    systemAppsArray.put(appInfo)
                } else {
                    userAppsArray.put(appInfo)
                }
            }

            // Add the categorized apps to the main JSONObject
            appMetrics.put("userApps", userAppsArray)
            appMetrics.put("systemApps", systemAppsArray)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return appMetrics
    }

    // Method to get detailed information for each app
    private fun getAppInfo(app: ApplicationInfo, packageManager: PackageManager): JSONObject {
        val appInfo = JSONObject()

        appInfo.put("appName", app.loadLabel(packageManager).toString())
        appInfo.put("packageName", app.packageName)
        appInfo.put("versionName", getVersionName(app.packageName, packageManager))
        appInfo.put("versionCode", getVersionCode(app.packageName, packageManager))
        appInfo.put("is64bit", is64BitApp(app))
        appInfo.put("apiLevel", getTargetSdk(app.targetSdkVersion))

        return appInfo
    }

    // Helper method to check if the app is a system app
    private fun isSystemApp(app: ApplicationInfo): Boolean {
        return (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    // Helper method to get version name
    private fun getVersionName(packageName: String, packageManager: PackageManager): String? {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    // Helper method to get version code
    private fun getVersionCode(packageName: String, packageManager: PackageManager): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            -1L
        }
    }

    // Helper method to check if the app is a 64-bit app
    private fun is64BitApp(app: ApplicationInfo): Boolean {
        return (app.nativeLibraryDir?.contains("arm64") == true || app.nativeLibraryDir?.contains("x86_64") == true)
    }

    // Helper method to get the target API level
    private fun getTargetSdk(targetSdkVersion: Int): String {
        return "API $targetSdkVersion"
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}