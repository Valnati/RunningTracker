package com.androiddevs.runningappyt.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.androiddevs.runningappyt.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

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
}
