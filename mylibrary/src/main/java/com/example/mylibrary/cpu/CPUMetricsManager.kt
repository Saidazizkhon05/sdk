package me.innerworks.iw_mobile_auth_android.cpu

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.widget.FrameLayout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class CPUMetricsManager(context: Context) {

    // Use WeakReference to hold the context to avoid memory leaks
    private val contextRef: WeakReference<Context> = WeakReference(context)

    // Public function to collect CPU and GPU metrics and return as a JSONObject
    fun getCPUMetrics(): JSONObject {
        val cpuMetrics = JSONObject()

        cpuMetrics.put("processor", getProcessor())
        cpuMetrics.put("frequency", getFrequency())
        cpuMetrics.put("supportedABIs", getSupportedABIs())
        cpuMetrics.put("cpuHardware", getCPUHardware())
        cpuMetrics.put("cpuGovernor", getCPUGovernor())
        cpuMetrics.put("cpuInfoPath", getCpuInfoPath())
        cpuMetrics.put("currentFrequency", getCurrentFrequency())

        contextRef.get()?.let { context ->
            getGPUInfoWithGLSurfaceView(context) { gpuRenderer, gpuVendor, gpuVersion ->
                cpuMetrics.put("gpuRenderer", gpuRenderer)
                cpuMetrics.put("gpuVendor", gpuVendor)
                cpuMetrics.put("gpuVersion", gpuVersion)
                cpuMetrics.put("gpuFrequency", getGPUFrequency())
            }
        }

        return cpuMetrics
    }

    // Private helper methods for collecting individual metrics dynamically

    private fun getProcessor(): String?{
        // Reads the processor information from /proc/cpuinfo
        return readFromFile("/proc/cpuinfo", "Processor")
    }

    private fun getFrequency(): String?{
        // Implement dynamic retrieval from CPU frequency files, if applicable
        // Example file: /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq
        return try {
            val maxFreq = readFromFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            val minFreq = readFromFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
            "$minFreq MHz - $maxFreq MHz"
        } catch (e: Exception) {
            null
        }
    }

    private fun getSupportedABIs(): String {
        // Get supported ABIs from Android API
        return Build.SUPPORTED_ABIS.joinToString(", ")
    }

    private fun getCPUHardware(): String? {
        // Reads the hardware information from /proc/cpuinfo
        return readFromFile("/proc/cpuinfo", "Hardware")
    }

    private fun getCPUGovernor(): String? {
        // Read CPU governor from the appropriate sysfs file
        return readFromFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
    }

    private fun getCpuInfoPath(): String {
        return "/proc/cpuinfo"
    }

    private fun getGPUInfoWithGLSurfaceView(context: Context, callback: (String?, String?, String?) -> Unit) {
        val glSurfaceView = GLSurfaceView(context)

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                val gpuRenderer = gl?.glGetString(GL10.GL_RENDERER)
                val gpuVendor = gl?.glGetString(GL10.GL_VENDOR)
                val gpuVersion = gl?.glGetString(GL10.GL_VERSION)
                callback(gpuRenderer, gpuVendor, gpuVersion)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })
    }

    private fun getGPUFrequency(): String? {
        val possiblePaths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpuclk",   // Qualcomm Adreno GPUs
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"  // Example for CPUs (but could apply to some GPUs)
        )

        for (path in possiblePaths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    return file.readText().trim()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getCurrentFrequency(): String? {
        // Return current frequency of CPU cores, retrieved dynamically if available
        return try {
            readFromFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
        } catch (e: Exception) {
            null
        }
    }

    // Helper function to read from system files
    private fun readFromFile(filePath: String, searchKey: String? = null): String? {
        return try {
            BufferedReader(FileReader(filePath)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (searchKey == null || line?.contains(searchKey) == true) {
                        return line?.split(":")?.get(1)?.trim()
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}