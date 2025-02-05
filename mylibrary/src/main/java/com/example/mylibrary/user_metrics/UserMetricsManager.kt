package me.innerworks.iw_mobile_auth_android.user_metrics

import android.content.Context
import android.view.MotionEvent
import android.view.ScrollCaptureSession
import android.view.View
import me.innerworks.iw_mobile_auth_android.user_metrics.touch_event.TouchMetricsManager
import me.innerworks.iw_mobile_auth_android.user_metrics.touch_event.TouchTrackingManager
import me.innerworks.iw_mobile_auth_android.utils.extentions.format
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

internal class UserMetricsManager{

    private lateinit var touchTrackingManager: TouchTrackingManager

    private var touchMetricsManager: TouchMetricsManager = TouchMetricsManager()

    fun handleTouchEvent(event: MotionEvent){
        touchMetricsManager.handleTouchEvent(event)
    }

    // Method to record text typing
    fun textOnChange(typing: String) {
        val timestamp = Date().format()
        try {
            if (typing.length > typed.length) { // Added
                val act = "added"
                val lastChar = typing[typing.length - 1]

                if (Character.isUpperCase(lastChar)) { // Uppercase added
                    actions.put(timestamp, "$act(uppercased)")
                } else if (!Character.isLetterOrDigit(lastChar) && !Character.isWhitespace(lastChar)) { // Symbols
                    actions.put(timestamp, "$act(symbols)")
                } else if (Character.isDigit(lastChar)) { // Number
                    actions.put(timestamp, "$act(number)")
                } else {
                    actions.put(timestamp, act)
                }
            } else { // Deleted
                actions.put(timestamp, "deleted")
            }
        } catch (exception: JSONException) {
            exception.printStackTrace()
        }
        typed = typing
    }
    private val actions = JSONObject()
    private var typed = ""


    fun getUserMetrics(): JSONObject{
        return JSONObject()
            .put("touch_event", touchMetricsManager.touchEvents)
            .put("typing_records", actions)

    }

}