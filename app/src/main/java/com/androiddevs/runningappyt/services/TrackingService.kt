package com.androiddevs.runningappyt.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getService
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.other.Constants.ACTION_PAUSE_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.androiddevs.runningappyt.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_STOP_SERVICE
import com.androiddevs.runningappyt.other.Constants.FASTEST_LOCATION_INTERVAL
import com.androiddevs.runningappyt.other.Constants.LOCATION_UPDATE_INTERVAL
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_CHANNEL_ID
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.androiddevs.runningappyt.other.Constants.NOTIFICATION_ID
import com.androiddevs.runningappyt.other.Constants.TIMER_UPDATE_INTERVAL
import com.androiddevs.runningappyt.other.TrackingUtility
import com.androiddevs.runningappyt.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    var isFirstRun = true

    var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeRunInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    //similar to static objects
    companion object {
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()

        //livedata of list of list of coordinates to allow for start/stop, noncontiguous times
        //the alias above makes this shorter to type
        val pathPoints = MutableLiveData<Polylines>()
    }

    //populate coordinates at the starting line
    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
       // fusedLocationProviderClient = FusedLocationProviderClient(this)
        //class is lifecycle owner so context is this
        isTracking.observe(this, {
            //observe both location and timer updates for the notification
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    //when run is fully stopped, destroy the service and reset everything
    private fun killService() {
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        //this will remove notification, and then kill service
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //if intent is not null
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        Timber.d("Resuming service")
                        //temporary fix to keep this running
                        startTimer()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    killService()
                }
            }
        }
        //called whenever intent w/ action is sent to service class
        return super.onStartCommand(intent, flags, startId)
        //need start, pause, stop actions
    }

    //observe time change and trigger liveData changes
    private var isTimerEnabled = false
    private var lapTime = 0L //time since start last pressed
    private var timeRun = 0L //total time during this run
    private var timeStarted = 0L //time started in millis
    private var lastSecondTimeStamp = 0L

    //is called whenever start button is clicked again
    private fun startTimer() {
        //start up data store
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        //use coroutine to track time, delay for few millis to calculate
        CoroutineScope(Dispatchers.Main).launch {
            while(isTracking.value!!) {
                //time diff between now and started
                lapTime = System.currentTimeMillis() - timeStarted
                //post the new laptime
                timeRunInMillis.postValue(timeRun + lapTime)
                //if it's been a second (checking last recorded time + 1 second)
                if(timeRunInMillis.value!! >= lastSecondTimeStamp + 1000L) {
                    //add new time and increment the last time this was done
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimeStamp += 1000L
                } //and delay every so often
                delay(TIMER_UPDATE_INTERVAL)
            }
            //when lap is completed, add it to total
            timeRun += lapTime
        }
    }

    private fun pauseService() {
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        //allows for update service through notification
        val notificationActionText = if(isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking) {
            //build intent to TrackingService
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            //send it off
            getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }
        //access the notification to update it
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //to avoid duplicate actions, must remove before adding (this is updating)
        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            //grabbing literally that text field here
            isAccessible = true
            //replace with an empty list of options
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        //finally build and send off under same id
        if (!serviceKilled) {

            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
        }

    }

    //request location updates if given permissions to do so
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest().apply {
                    //theoretically every 5 seconds update, but this is not guaranteed
                    //def every 2 seconds though, that is defined in constants
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
            //have stopped tracking so remove
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    //handles gathering location changes and adding to polylines
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            //result should not be null, as it's being delivered to us
            if (isTracking.value!!) {
                //these could be though
                result.locations.let { locations ->
                    //if not add every location to current list
                    for (location in locations) {
                        addPathPoint(location)
                        Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    //add location to last polyline of the current list of polylines
    private fun addPathPoint(location: Location) {
        //if it is not null
        location.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        } //if null do nothing
    }

    //on a pause, add empty list to populate when next starting
    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        //broadcast new value of empty list
        pathPoints.postValue(this)
        //if list is null, add empty list and empty polyline to that list
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))


    //create and send notification
    private fun startForegroundService() {
        startTimer()
        //change mutable data with its own method
        isTracking.postValue(true)
        //grab straight out of the framework and cast to a manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        //easy check to avoid chrash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        //standard builder pattern
        //with no way to disappear the service on touch (cancel), swipe away (ongoing)
        //using dependency injection to remove some boilerplate, see TrackingService
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())
        //builder now includes building the intent thanks to injection
        timeRunInSeconds.observe(this, {
            if(!serviceKilled) {
                //only show notification if service is still alive, avoid calling observer after killing
                val notification = curNotificationBuilder
                    //multiply the seconds result and ignore last argument, so won't include millis
                    .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }

    //build channel with constants and start it in the manager
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}