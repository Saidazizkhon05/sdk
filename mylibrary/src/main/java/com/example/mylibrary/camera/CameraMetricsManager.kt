package me.innerworks.iw_mobile_auth_android.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Range
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class CameraMetricsManager(context: Context) {

    private val contextRef = WeakReference(context)

    // Public method to get camera metrics for all cameras as a JSONObject
    fun getCameraMetrics(): JSONObject {
        val cameraMetrics = JSONObject()
        val context = contextRef.get() ?: return errorResult("Context is not available")

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList

            for (cameraId in cameraIds) {
                val cameraInfo = getCameraInfo(cameraManager, cameraId)
                cameraMetrics.put("Camera $cameraId", cameraInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cameraMetrics
    }

    // Private method to get detailed camera information for a specific camera ID
    private fun getCameraInfo(cameraManager: CameraManager, cameraId: String): JSONObject {
        val cameraInfo = JSONObject()

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        // Camera lens facing direction
        val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
        val lensFacingString = when (lensFacing) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            else -> "Front"
        }

        cameraInfo.put("lensFacing", lensFacingString)

        // Camera sensor size and resolution
        val pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val sensorResolution = "${pixelArraySize?.width}x${pixelArraySize?.height}"
        cameraInfo.put("sensorResolution", sensorResolution)

        // Focal length
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        cameraInfo.put("focalLength", focalLengths?.get(0)?.toString())

        // Auto exposure modes
        val autoExposureModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        cameraInfo.put("autoExposureModes", autoExposureModes?.let { getExposureModes(it) })

        // Auto focus modes
        val autoFocusModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        cameraInfo.put("autoFocusModes", autoFocusModes?.let { getFocusModes(it) })

        // Scene modes
        val sceneModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
        cameraInfo.put("sceneModes", sceneModes?.let { getSceneModes(it) })

        // Target FPS ranges
        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        cameraInfo.put("targetFpsRanges", fpsRanges?.let { getFpsRanges(it) })

        // Compensation range and step
        val compensationRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val compensationStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toDouble()
        cameraInfo.put("compensationRange", compensationRange?.toString())
        cameraInfo.put("compensationStep", compensationStep?.toString())

        // HDR Support (Checking HDR capability) NEED TO BE CHECKED
        val availableCapabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val hdrSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            availableCapabilities?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR)
        } else {
            false
        }
        cameraInfo.put("hdrSupport", if (hdrSupported == true) "Supported" else "Not Supported")

        // Additional modes
        val aberrationModes = characteristics.get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)
        cameraInfo.put("aberrationModes", aberrationModes?.let { getAberrationModes(it) })

        val antibandingModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES)
        cameraInfo.put("antibandingModes", antibandingModes?.let { getAntibandingModes(it) })

        return cameraInfo
    }

    // Helper method to convert auto exposure modes to a readable format
    private fun getExposureModes(modes: IntArray): JSONArray {
        val exposureModes = JSONArray()
        modes.forEach {
            exposureModes.put(
                when (it) {
                    CameraCharacteristics.CONTROL_AE_MODE_OFF -> "Off"
                    CameraCharacteristics.CONTROL_AE_MODE_ON -> "On"
                    CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH -> "Auto Flash"
                    CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> "Always Flash"
                    CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> "Auto Redeye"
                    else -> "Unknown"
                }
            )
        }
        return exposureModes
    }

    // Helper method to convert auto focus modes to a readable format
    private fun getFocusModes(modes: IntArray): JSONArray {
        val focusModes = JSONArray()
        modes.forEach {
            focusModes.put(
                when (it) {
                    CameraCharacteristics.CONTROL_AF_MODE_OFF -> "Off"
                    CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "Auto"
                    CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "Continuous Picture"
                    CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "Continuous Video"
                    else -> "Unknown"
                }
            )
        }
        return focusModes
    }

    // Helper method to convert scene modes to a readable format
    private fun getSceneModes(modes: IntArray): JSONArray {
        val sceneModes = JSONArray()
        modes.forEach {
            sceneModes.put(
                when (it) {
                    CameraCharacteristics.CONTROL_SCENE_MODE_FACE_PRIORITY -> "Face Priority"
                    CameraCharacteristics.CONTROL_SCENE_MODE_ACTION -> "Action"
                    CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT -> "Portrait"
                    CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE -> "Landscape"
                    else -> "Unknown"
                }
            )
        }
        return sceneModes
    }

    // Helper method to convert FPS ranges to a readable format
    private fun getFpsRanges(fpsRanges: Array<Range<Int>>): JSONArray {
        val fpsRangesArray = JSONArray()
        fpsRanges.forEach {
            fpsRangesArray.put("[${it.lower}, ${it.upper}]")
        }
        return fpsRangesArray
    }

    // Helper method to convert aberration modes to a readable format
    private fun getAberrationModes(modes: IntArray): JSONArray {
        val aberrationModes = JSONArray()
        modes.forEach {
            aberrationModes.put(
                when (it) {
                    CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_OFF -> "Off"
                    CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_FAST -> "Fast"
                    CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY -> "High Quality"
                    else -> "Unknown"
                }
            )
        }
        return aberrationModes
    }

    // Helper method to convert antibanding modes to a readable format
    private fun getAntibandingModes(modes: IntArray): JSONArray {
        val antibandingModes = JSONArray()
        modes.forEach {
            antibandingModes.put(
                when (it) {
                    CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_OFF -> "Off"
                    CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_50HZ -> "50Hz"
                    CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_60HZ -> "60Hz"
                    CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_AUTO -> "Auto"
                    else -> "Unknown"
                }
            )
        }
        return antibandingModes
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}