package me.innerworks.iw_mobile_auth_android.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import me.innerworks.iw_mobile_auth_android.utils.threading.safe.safeWithTimeout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL

internal class NetworkMetricsManager(context: Context) {

    private val contextRef: WeakReference<Context> = WeakReference(context)

    @SuppressLint("MissingPermission")
    fun getNetworkMetrics(): JSONObject? {
        val networkMetrics = JSONObject()
        val context = contextRef.get() ?: return errorResult("Context is not available")
        networkMetrics.put("isUsingVPN", isUsingVPN(context))
        safeWithTimeout{
            networkMetrics.put("ipInfo", getIpInfo())
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return networkMetrics
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } else {
            // For pre-Marshmallow devices, use NetworkInfo
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI->{
                        val wifiMetrics = getWiFiNetworkMetrics(context)
                        networkMetrics.put("connectionType", "Wi-Fi")
                        networkMetrics.put("wifiMetrics", wifiMetrics)
                    }
                    ConnectivityManager.TYPE_MOBILE -> {
                        val mobileDataMetrics = getMobileDataMetrics(context)
                        networkMetrics.put("connectionType", "Cellular")
                        networkMetrics.put("mobileDataMetrics", mobileDataMetrics)
                    }
                    else -> return networkMetrics
                }
            } else {
                return networkMetrics
            }
            return networkMetrics
        }

        when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                val wifiMetrics = getWiFiNetworkMetrics(context)
                networkMetrics.put("connectionType", "Wi-Fi")
                networkMetrics.put("wifiMetrics", wifiMetrics)
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                val mobileDataMetrics = getMobileDataMetrics(context)
                networkMetrics.put("connectionType", "Cellular")
                networkMetrics.put("mobileDataMetrics", mobileDataMetrics)
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> {
                val ethernetMetrics = getEthernetMetrics(context)
                networkMetrics.put("connectionType", "Ethernet")
                networkMetrics.put("ethernetMetrics", ethernetMetrics)
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true -> {
                val bluetoothMetrics = getBluetoothMetrics(context)
                networkMetrics.put("connectionType", "Bluetooth")
                networkMetrics.put("bluetoothMetrics", bluetoothMetrics)
            }
            else -> {
                networkMetrics.put("connectionType", "No Internet Connection")
            }
        }

        return networkMetrics
    }

    // Method to retrieve Wi-Fi metrics (no additional permissions required)

    @SuppressLint("MissingPermission")
    private fun getWiFiNetworkMetrics(context: Context): JSONObject? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val wifiMetrics = JSONObject()
        // Get ConnectivityManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Check if connected to a WiFi network
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // WiFi is connected
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiManager.connectionInfo

                wifiMetrics.put("SSID", wifiInfo.ssid)
                wifiMetrics.put("BSSID", wifiInfo.bssid)
                wifiMetrics.put("LinkSpeedMbps", wifiInfo.linkSpeed)
                wifiMetrics.put("FrequencyMHz", wifiInfo.frequency)
                wifiMetrics.put("SignalStrength", WifiManager.calculateSignalLevel(wifiInfo.rssi, 100))
                wifiMetrics.put("IP Address", formatIpAddress(wifiInfo.ipAddress))

                // Get Gateway and DNS addresses (if possible)
                val dhcpInfo = wifiManager.dhcpInfo
                wifiMetrics.put("Gateway", formatIpAddress(dhcpInfo.gateway))
                wifiMetrics.put("DNS1", formatIpAddress(dhcpInfo.dns1))
                wifiMetrics.put("DNS2", formatIpAddress(dhcpInfo.dns2))
            }
        } else {
            // For older devices (API < 23), use NetworkInfo and WifiManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                // WiFi is connected
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiManager.connectionInfo

                wifiMetrics.put("SSID", wifiInfo.ssid)
                wifiMetrics.put("BSSID", wifiInfo.bssid)
                wifiMetrics.put("LinkSpeedMbps", wifiInfo.linkSpeed)
                wifiMetrics.put("FrequencyMHz", wifiInfo.frequency)
                wifiMetrics.put("SignalStrength", WifiManager.calculateSignalLevel(wifiInfo.rssi, 100))
                wifiMetrics.put("IP Address", formatIpAddress(wifiInfo.ipAddress))

                // Get Gateway and DNS addresses (if possible)
                val dhcpInfo = wifiManager.dhcpInfo
                wifiMetrics.put("Gateway", formatIpAddress(dhcpInfo.gateway))
                wifiMetrics.put("DNS1", formatIpAddress(dhcpInfo.dns1))
                wifiMetrics.put("DNS2", formatIpAddress(dhcpInfo.dns2))
            }
        }

        return wifiMetrics
    }

    // Helper function to format IP address from int to human-readable format
    @SuppressLint("DefaultLocale")
    private fun formatIpAddress(ip: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            (ip and 0xFF),
            (ip shr 8 and 0xFF),
            (ip shr 16 and 0xFF),
            (ip shr 24 and 0xFF)
        )
    }

    private fun getIpInfo(): JSONObject {
        var ipInfo = JSONObject()
        try {
            val url = URL("https://ipinfo.io/json/") // alternative: "https://ipinfo.io/json/"
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            // Read the entire response from the input stream
            val inStream = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var inputLine: String?
            while (inStream.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            inStream.close()
            // Check if the response is not empty before parsing
            if (response.isNotEmpty()) {
                ipInfo = JSONObject(response.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ipInfo = JSONObject() // Return an empty JSONObject on error
        }
        return ipInfo
    }

    // Method to retrieve Mobile Data metrics, with permission check
    @SuppressLint("MissingPermission")
    private fun getMobileDataMetrics(context: Context): JSONObject {
        val mobileDataMetrics = JSONObject()
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            mobileDataMetrics.put("deviceType", getNetworkType(telephonyManager.networkType))
            mobileDataMetrics.put("APN", telephonyManager.simOperatorName)
            // Add other mobile data metrics here...
        } else {
            mobileDataMetrics.put("error", "Permission not granted for Mobile Data metrics.")
        }

        return mobileDataMetrics
    }

    // Method to retrieve Ethernet connection metrics (no additional permissions required)
    @SuppressLint("MissingPermission")
    private fun getEthernetMetrics(context: Context): JSONObject {
        val ethernetMetrics = JSONObject()

        // For API level >= M (Android 6.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)

            ethernetMetrics.put("IP", linkProperties?.linkAddresses?.getOrNull(0)?.address?.hostAddress)
            ethernetMetrics.put("DNS1", linkProperties?.dnsServers?.getOrNull(0)?.hostAddress)
            ethernetMetrics.put("DNS2", linkProperties?.dnsServers?.getOrNull(1)?.hostAddress)
            ethernetMetrics.put("gateway", linkProperties?.routes?.getOrNull(0)?.gateway?.hostAddress)
            ethernetMetrics.put("interface", linkProperties?.interfaceName)
        } else { // For API level < M (Android 6.0)

            // Use java.net.NetworkInterface as a fallback for lower SDKs
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()

                // Iterate over all network interfaces to find Ethernet-related interfaces
                for (networkInterface in networkInterfaces) {
                    if (!networkInterface.isLoopback && networkInterface.isUp && networkInterface.name.startsWith("eth")) {
                        ethernetMetrics.put("interface", networkInterface.name)
                        // Collect IP addresses
                        val inetAddresses = networkInterface.inetAddresses.toList()
                        val ipAddresses = inetAddresses.mapNotNull { it.hostAddress }
                        ethernetMetrics.put("IP", ipAddresses.getOrNull(0))
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                ethernetMetrics.put("error", "Failed to retrieve ethernet metrics: ${e.message}")
            }
        }

        return ethernetMetrics
    }

    // Method to retrieve Bluetooth connection metrics, with suppressed permission lint warning
    @SuppressLint("MissingPermission")
    private fun getBluetoothMetrics(context: Context): JSONObject {
        val bluetoothMetrics = JSONObject()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            bluetoothMetrics.put("status", "Enabled")
            bluetoothMetrics.put("deviceName", bluetoothAdapter.name)
            bluetoothMetrics.put("address", bluetoothAdapter.address)
            bluetoothMetrics.put("pairedDevices", getPairedBluetoothDevices(bluetoothAdapter))
        } else {
            bluetoothMetrics.put("status", "Disabled")
        }

        return bluetoothMetrics
    }

    // Helper method to retrieve paired Bluetooth devices
    @SuppressLint("MissingPermission")
    private fun getPairedBluetoothDevices(bluetoothAdapter: BluetoothAdapter): JSONObject {
        val pairedDevicesMetrics = JSONObject()
        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            val deviceInfo = JSONObject()
            deviceInfo.put("name", device.name)
            deviceInfo.put("address", device.address)
            pairedDevicesMetrics.put(device.address, deviceInfo)
        }
        return pairedDevicesMetrics
    }

    // Helper method to get network type (2G, 3G, 4G, 5G)
    private fun getNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            else -> "Unknown"
        }
    }

    @SuppressLint("MissingPermission")
    private fun isUsingVPN(context: Context): Boolean {
        // Check for permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            return false
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                // For Android N and above (API 24+)
                val activeNetwork: Network? = cm.activeNetwork
                val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // For Android M and above
                val activeNetwork: Network? = cm.activeNetwork
                val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
            else -> {
                // For Android Lollipop and below
                val networks = cm.allNetworks
                for (network in networks) {
                    val capabilities = cm.getNetworkCapabilities(network)
                    if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                        return true
                    }
                }
                false
            }
        }
    }

    // Private helper method to return an error in JSON format
    private fun errorResult(message: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", message)
        return errorJson
    }
}