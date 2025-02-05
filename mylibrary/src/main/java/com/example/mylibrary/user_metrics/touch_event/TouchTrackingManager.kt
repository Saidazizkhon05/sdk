package me.innerworks.iw_mobile_auth_android.user_metrics.touch_event

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

internal class TouchTrackingManager(view: View) {

    private val touchEvents = mutableListOf<JSONObject>()
    private var touchStartTime: Long = 0
    private val viewRef: WeakReference<View> = WeakReference(view)

    private val ACTION_DOWN = "ACTION_DOWN"
    private val ACTION_MOVE = "ACTION_MOVE"
    private val ACTION_UP = "ACTION_UP"
    private val CLICK = "CLICK"
    private val MAX_MOVEMENT = 10.0f
    private val MAX_DURATION = 200L


    init {
        startTouchTracking()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startTouchTracking() {
        val view = viewRef.get()
        view?.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event?.let {
                    try {
                        val touchEvent = JSONObject()
                        val currentTime = SystemClock.elapsedRealtime()
                        val pressure = it.pressure
                        val size = it.size

                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                startX = it.x
                                startY = it.y
                                touchStartTime = currentTime
                                populateTouchEvent(touchEvent, ACTION_DOWN, it.x, it.y, currentTime, pressure, size)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                populateTouchEvent(touchEvent, ACTION_MOVE, it.x, it.y, currentTime, pressure, size)
                            }
                            MotionEvent.ACTION_UP -> {
                                val endX = it.x
                                val endY = it.y
                                populateTouchEvent(touchEvent, ACTION_UP, endX, endY, currentTime, pressure, size)
                                touchEvent.put("duration", currentTime - touchStartTime)
                                if (isClick(startX, startY, endX, endY, touchStartTime, currentTime)) {
                                    touchEvent.put("event", CLICK)
                                }
                            }
                        }
                        touchEvents.add(touchEvent)
                    } catch (e: JSONException) {
                        Log.e("TouchTrackingManager", "Error logging touch event", e)
                    }
                }
                return false // Allow the event to be processed by the view as well
            }
        })
    }

    private fun populateTouchEvent(
        touchEvent: JSONObject,
        eventType: String,
        x: Float,
        y: Float,
        timestamp: Long,
        pressure: Float,
        size: Float
    ) {
        try {
            touchEvent.put("event", eventType)
            touchEvent.put("x", x)
            touchEvent.put("y", y)
            touchEvent.put("timestamp", timestamp)
            touchEvent.put("pressure", pressure)
            touchEvent.put("size", size)
        } catch (e: JSONException) {
            Log.e("TouchTrackingManager", "Error populating touch event", e)
        }
    }

    fun getTouchEvents(): JSONArray {
        return JSONArray(touchEvents)
    }

    fun clear() {
        touchEvents.clear()
    }

    private fun isClick(startX: Float, startY: Float, endX: Float, endY: Float, startTime: Long, endTime: Long): Boolean {
        val distance = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val duration = endTime - startTime
        return distance <= MAX_MOVEMENT && duration <= MAX_DURATION
    }
}