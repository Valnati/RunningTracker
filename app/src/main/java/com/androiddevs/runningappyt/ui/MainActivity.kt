package com.androiddevs.runningappyt.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    //want to inject something, so dagger looks for this type, and find it in the AppModule object
    //this is then injected, after creation (through the provideRunningDatabase function)
    //you made the content stream to build this object, and then called it to where it needs to go
    //Dagger does everything else automatically
    //@Inject tells where to place, @AndroidEntryPoint tells what wants injection,
    //@Module gives space to build, @InstallIn(SingletonComponent) defines the scope,
    //@Provides marks that this function will give a resulting object
    //@ApplicationContext defines the context to be used when building

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //normally don't provide injection in the main activity - do it in the repository (MVVM)
        setSupportActionBar(toolbar)
        //does the intent start a new activity?
        navigateToTrackingFragmentIfNeeded(intent)

        //assign navigation ability to bottomNaveBar
        bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
        //remove menu as needed for the different fragments
        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when(destination.id) {
                    R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment ->
                        bottomNavigationView.visibility = View.VISIBLE
                    else -> bottomNavigationView.visibility = View.INVISIBLE
                }
            }


    }

    //if the activity isn't new, it skips onCreate, so gotta do it here instead
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    //if get sent intent with id, go to there
    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        Timber.d("Intent found")
        if(intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            navHostFragment.findNavController().navigate(R.id.action_global_tracking_fragment)
            Timber.d("restored via intent")
        }
    }
}
