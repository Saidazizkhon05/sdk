package me.innerworks.iw_mobile_auth_android.utils.extentions


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val DEFAULT_PATTERN = "yyyy.MM.dd.HH.mm.ss.SSS"
fun Date.format(format:String = DEFAULT_PATTERN): String = SimpleDateFormat(format, Locale.getDefault()).format(this)
