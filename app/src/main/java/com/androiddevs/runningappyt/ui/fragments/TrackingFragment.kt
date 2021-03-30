package com.androiddevs.runningappyt.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.other.Constants.ACTION_PAUSE_SERVICE
import com.androiddevs.runningappyt.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.androiddevs.runningappyt.other.Constants.MAP_ZOOM
import com.androiddevs.runningappyt.other.Constants.POLYLINE_COLOR
import com.androiddevs.runningappyt.other.Constants.POLYLINE_WIDTH
import com.androiddevs.runningappyt.services.Polyline
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var map: GoogleMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
        btnToggleRun.setOnClickListener {
            //action now in this method
            toggleRun()
        }
        //get map off main thread
        mapView.getMapAsync {
            map = it
            //if redrawing, recreate the screen lines from saved data
            addAllPolylines()
        }

        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        //pass owner to fragment and update screen
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })
        //here on add new coordinates to screen
        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })
    }

    //use commands to notify intent of current state, alter notification
    private fun toggleRun() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    //responsible for changing screen to show stop/start state
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        //if paused
        if(!isTracking) {
            btnToggleRun.text = getString(R.string.text_start)
            btnFinishRun.visibility = View.VISIBLE
        } else {
            btnToggleRun.text = getString(R.string.text_stop)
            btnFinishRun.visibility = View.INVISIBLE
        }
    }

    //moves the camera based on constants available
    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    //loop through existing points and add them when screen is redrawn
    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    //when path points change, connect the new one and the previous together
    private fun addLatestPolyline() {
        //if new points exist and last point has something to connect to
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            //get 2nd to last and last
            val preLastLatLong = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLong = pathPoints.last().last()
            //put in polyline and add to map, if map exists
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLong)
                .add(lastLatLong)
            map?.addPolyline(polylineOptions)
        }
    }

    //this doesn't start every time but only sends intent to service
    private fun sendCommandToService(action: String) =
        //build intent with context, destination, and also the action string
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    //mapView is attached to fragment's view, so that's the lifecycle to care about
    //fragments have
    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}