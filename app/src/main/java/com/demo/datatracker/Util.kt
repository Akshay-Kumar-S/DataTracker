package com.demo.datatracker

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Util {
    private val TAG = "dataTracker"

    fun getUid(appName: String): Int {
        return try {
            App.getInstance().packageManager.getApplicationInfo(appName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
           0
        }
    }

    fun getDateDefault(milliSeconds: Long): String {
        return getDate(milliSeconds, "dd/MM/yyyy HH:mm:ss.SSS")
    }

    private fun getDate(milliSeconds: Long, format: String): String {
        val formatter = SimpleDateFormat(format)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
    }

    fun getStartTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 19)
        date.set(Calendar.MINUTE, 40)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)
        return date.timeInMillis
    }

    fun getEndTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 19)
        date.set(Calendar.MINUTE, 50)
        date.set(Calendar.SECOND, 23)
        date.set(Calendar.MILLISECOND, 430)
        return date.timeInMillis
    }

    fun getSimSubscriberId(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                return null
            val tm =
                App.getInstance().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return tm.subscriberId
        } catch (e: Exception) {
        }
        return ""
    }
}
