package me.innerworks.iw_mobile_auth_android.display

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

internal class DisplayMetricsManager(context: Context) {

    private val contextRef = WeakReference(context)

    // Public method to get display metrics as JSONObject
    fun getDisplayMetrics(): JSONObject {
        val displayMetrics = JSONObject()
        val context = contextRef.get() ?: return errorResult("Context is not available")

        // Get WindowManager and DisplayMetrics
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        // Get screen width, height, and physical size
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val xdpi = metrics.xdpi
        val ydpi = metrics.ydpi
        val physicalSizeInches = calculatePhysicalSize(screenWidth, screenHeight, xdpi, ydpi)

        displayMetrics.put("screenWidth", "$screenWidth px")
        displayMetrics.put("screenHeight", "$screenHeight px")
        displayMetrics.put("physicalSize", String.format("%.2f inches", physicalSizeInches))

        // Get display bucket (density qualifier)
        displayMetrics.put("displayBucket", getDensityBucket(metrics))

        // Get dpi values (dots per inch)
        displayMetrics.put("displayDpi", "${metrics.densityDpi} dpi")
        displayMetrics.put("xdpi", "${metrics.xdpi} dpi")
        displayMetrics.put("ydpi", "${metrics.ydpi} dpi")

        // Get logical and scaled density
        displayMetrics.put("logicalDensity", metrics.density)
        displayMetrics.put("scaledDensity", metrics.scaledDensity)

        // Get screen orientation
        val orientation = context.resources.configuration.orientation
        val orientationString = if (orientation == Configuration.ORIENTATION_LANDSCAPE) "Landscape" else "Portrait"
        displayMetrics.put("defaultOrientation", orientationString)

        // Get refresh rate
        val refreshRate = windowManager.defaultDisplay.refreshRate
        displayMetrics.put("refreshRate", "$refreshRate Hz")

        // Check if HDR is supported
        val hdrSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                windowManager.defaultDisplay.isHdr
            } else {
                false
                TODO("VERSION.SDK_INT < O")
            }
        } else {
            false
        }
        displayMetrics.put("HDR", if (hdrSupported) "Supported" else "Not Supported")

        // Get brightness mode (manual or automatic)
        val brightnessMode = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, -1)
        val brightnessModeString = if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            "Automatic"
        } else {
            "Manual"
        }
        displayMetrics.put("brightnessMode", brightnessModeString)

        // Get screen timeout
        val screenTimeout = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1)
        displayMetrics.put("screenTimeout", "$screenTimeout Seconds")

        // Get font scale
        val fontScale = context.resources.configuration.fontScale
        displayMetrics.put("fontScale", fontScale)

        return displayMetrics
    }

    // Method to calculate physical screen size
    private fun calculatePhysicalSize(screenWidth: Int, screenHeight: Int, xdpi: Float, ydpi: Float): Double {
        val widthInInches = screenWidth / xdpi
        val heightInInches = screenHeight / ydpi
        return sqrt((widthInInches.pow(2) + heightInInches.pow(2)).toDouble())
    }

    // Method to get density bucket (density qualifier like ldpi, mdpi, hdpi, etc.)
    private fun getDensityBucket(metrics: DisplayMetrics): String {
        return when (metrics.densityDpi) {
            in 0..120 -> "ldpi"
            in 121..160 -> "mdpi"
            in 161..240 -> "hdpi"
            in 241..320 -> "xhdpi"
            in 321..480 -> "xxhdpi"
            in 481..640 -> "xxxhdpi"
            else -> "Unknown"
        }
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}