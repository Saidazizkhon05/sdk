package me.innerworks.iw_mobile_auth_android

import android.view.MotionEvent
import org.json.JSONObject

interface InnerworksMetrics {
    fun sendCollectedData(data: JSONObject, onFail:(msg: String)->Unit, onSuccess:()->Unit)
    fun textOnChange(typing:String)
    fun sendTouchEvent(event:MotionEvent)
    fun setOptionalData(data: JSONObject)
    fun setBaseUrl(url: String)
}