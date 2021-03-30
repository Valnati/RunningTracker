package com.androiddevs.runningappyt.other

import android.content.Context
import android.os.Build
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.min

object TrackingUtility {

    fun hasLocationPermissions(context: Context) =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    //will take time data and convert it to a formatted String for notification, view
    fun getFormattedStopWatchTime(ms: Long, includeMillis: Boolean = false): String {
        var millis = ms
        //this is like modulo - record largest unit, then remove it from pile of unrecorded time
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        millis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        millis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        if(!includeMillis) {
            return "${if(hours<10) "0" else ""}$hours:" +
                    "${if(minutes<10) "0" else ""}$minutes:" +
                    "${if(seconds<10) "0" else ""}$seconds"
        }
        millis -= TimeUnit.SECONDS.toMillis(seconds)
//        Timber.d("Millis before conversion: $millis")
        millis /= 10 //remove third digit
//        Timber.d("Millis before conversion: $millis")
        return "${if(hours<10) "0" else ""}$hours:" +
                "${if(minutes<10) "0" else ""}$minutes:" +
                "${if(seconds<10) "0" else ""}$seconds:" +
                "${if(millis<10) "0" else ""}$millis"
    }
}