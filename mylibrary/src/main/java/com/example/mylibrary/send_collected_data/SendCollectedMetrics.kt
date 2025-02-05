package me.innerworks.iw_mobile_auth_android.send_collected_data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


internal class SendCollectedMetrics(context: Context) {
    private val URL = "/innerworks/mobile-metrics/non-auth-frontend-flow"
    private var BASE_URL: String = "https://api.prod.innerworks.me/api/v1"
    private var contextReference: WeakReference<Context> = WeakReference(context)

    fun setBaseUrl(baseUrl: String){
        BASE_URL = baseUrl
    }

    fun sendPostRequest(payload: JSONObject, onFail: ((msg: String) -> Unit), onSuccess: (() -> Unit)) {
        if (!isNetworkAvailable) {
            onFail("Network is not available")
            return
        }

        Thread {
            var conn: HttpURLConnection? = null
            var reader: BufferedReader? = null
            try {
                val url = URL(BASE_URL + URL)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "*/*")
                conn.doOutput = true
                conn.connectTimeout = 10000 // 10 seconds
                conn.readTimeout = 10000 // 10 seconds

                conn.outputStream.use { os ->
                    val input =
                        payload.toString().toByteArray(StandardCharsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                val responseCode = conn.responseCode
                Log.d(TAG, "Response Code: $responseCode")

                val response = StringBuilder()
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    reader = BufferedReader(InputStreamReader(conn.inputStream))
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        response.append(line)
                    }
                    onSuccess.invoke()
                    Log.d(TAG, "Response: $response")
                } else {
                    onFail("HTTP Error Response: " + readErrorResponse(conn))
                }
            } catch (e: Exception) {
                onFail("Error sending HTTP request: ${e.message}")
            } finally {
                conn?.disconnect()
                try {
                    reader?.close()
                    onSuccess.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing reader", e)
                }
            }
        }.start()
    }

    private fun readErrorResponse(conn: HttpURLConnection?): String {
        val errorResponse = StringBuilder()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(conn!!.errorStream))
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                errorResponse.append(line)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading error response", e)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing reader", e)
                }
            }
        }
        return errorResponse.toString()
    }

    @get:SuppressLint("MissingPermission")
    private val isNetworkAvailable: Boolean get() {
        val safeContext = contextReference.get() ?: return false
        if (ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val connectivityManager = safeContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
    companion object {
        private const val TAG = "SendCollectedMetrics"
    }
}