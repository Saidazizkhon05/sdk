package me.innerworks.iw_mobile_auth_android.device.biometics

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.os.Build
import java.lang.Exception
import java.lang.reflect.Method
import java.lang.ref.WeakReference

internal class BiometricMetricsManager(context: Context) {

    private val contextReference = WeakReference(context)

    fun getBiometricAuthDetails(callback: (Map<String, Any>) -> Unit){
        val biometricDetails = HashMap<String, Any>()
        val isAvailable = isBiometricAuthAvailable()
        biometricDetails["BiometricAvailable"] = isAvailable

        if (isAvailable) {
            biometricDetails["BiometricType"] = getBiometricAuthType() ?: "null"
        }

        callback(biometricDetails)
    }

    private fun isBiometricAuthAvailable(): Boolean {
        val context = contextReference.get() ?: return false

        return try {
            // Use reflection to check if BiometricManager class is available
            val biometricManagerClass = Class.forName("androidx.biometric.BiometricManager")
            val fromMethod = biometricManagerClass.getMethod("from", Context::class.java)
            val biometricManager = fromMethod.invoke(null, context)

            val canAuthenticateMethod: Method
            val canAuthenticate: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29) and above
                canAuthenticateMethod = biometricManagerClass.getMethod("canAuthenticate", Int::class.javaPrimitiveType)
                canAuthenticate = canAuthenticateMethod.invoke(
                    biometricManager,
                    getBiometricAuthenticators()
                ) as Int
                canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                // For SDK < 29, fallback to checking biometric capabilities without using the constant
                canAuthenticateMethod = biometricManagerClass.getMethod("canAuthenticate")
                canAuthenticate = canAuthenticateMethod.invoke(biometricManager) as Int
                canAuthenticate == 0 // Typically, 0 is success for older SDKs
            }
        } catch (e: Exception) {
            false // BiometricManager is not available or something went wrong
        }
    }

    private fun getBiometricAuthType(): String? {
        val context = contextReference.get() ?: return null

        return try {
            // Use reflection to check if BiometricManager class is available
            val biometricManagerClass = Class.forName("androidx.biometric.BiometricManager")
            val fromMethod = biometricManagerClass.getMethod("from", Context::class.java)
            val biometricManager = fromMethod.invoke(null, context)

            val canAuthenticateMethod: Method
            val authType: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 (API 29) and above
                canAuthenticateMethod = biometricManagerClass.getMethod("canAuthenticate", Int::class.javaPrimitiveType)
                authType = canAuthenticateMethod.invoke(
                    biometricManager,
                    getBiometricAuthenticators()
                ) as Int
            } else {
                // For SDK < 29, fallback to checking biometric capabilities without using the constant
                canAuthenticateMethod = biometricManagerClass.getMethod("canAuthenticate")
                authType = canAuthenticateMethod.invoke(biometricManager) as Int
            }

            determineBiometricType(authType)
        } catch (e: Exception) {
            null
        }
    }

    private fun determineBiometricType(authType: Int): String {
        return when (authType) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric authentication available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric enrolled"
            else -> if (authType == 0) "Biometric authentication available (Legacy)" else "Unknown"
        }
    }

    private fun getBiometricAuthenticators(): Int {
        return BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}