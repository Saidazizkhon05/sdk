package me.innerworks.iw_mobile_auth_android.system.root_checker

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Method

internal class RootCheckManager(context: Context) {

    private val contextReference = WeakReference(context)

    fun isRooted(): Boolean {
        return isSuBinaryPresent() ||
                isSuperuserAppPresent() ||
                isSystemBinaryModified() ||
                isTamperedSystemProperties()
    }

    private fun isSuBinaryPresent(): Boolean {
        val paths = arrayOf(
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun isSuperuserAppPresent(): Boolean {
        val knownRootApps = arrayOf(
            "com.noshufou.android.su",      // SuperSU
            "eu.chainfire.supersu",         // SuperSU
            "me.phh.superuser",             // Superuser
            "com.koushikdutta.superuser"    // Superuser (Koush)
        )
        val context = contextReference.get() ?: return false
        val pm: PackageManager = context.packageManager
        for (app in knownRootApps) {
            try {
                pm.getPackageInfo(app, PackageManager.GET_ACTIVITIES)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // App not installed
            }
        }
        return false
    }

    private fun isSystemBinaryModified(): Boolean {
        val binaries = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su"
        )
        for (binary in binaries) {
            val file = File(binary)
            if (file.exists() && !file.canExecute()) {
                return true
            }
        }
        return false
    }

    private fun isTamperedSystemProperties(): Boolean {
        val propertiesToCheck = arrayOf(
            "ro.build.tags",        // Check build tags
            "ro.debuggable",        // Check if the build is debuggable
            "ro.build.type"         // Check build type
        )
        for (property in propertiesToCheck) {
            val value = getSystemProperty(property)
            if (value != null && (value.contains("test-keys") || value.contains("userdebug") || value.contains("debug"))) {
                return true
            }
        }
        return false
    }

    private fun getSystemProperty(property: String): String? {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemPropertiesClass.getMethod("get", String::class.java)
            getMethod.invoke(null, property) as String
        } catch (e: Exception) {
            Log.e("RootCheckManager", "Error getting system property: $e")
            null
        }
    }
}