package me.innerworks.iw_mobile_auth_android

import android.content.Context

class InnerworksMetricsFactory private constructor(){
    companion object{
        @JvmStatic
        fun create(context: Context): InnerworksMetrics{
            return InnerworksMetricsManager(context)
        }
    }
}