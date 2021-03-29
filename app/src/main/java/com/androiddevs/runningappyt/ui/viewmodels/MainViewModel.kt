package com.androiddevs.runningappyt.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.androiddevs.runningappyt.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

//beta changed ViewModelInject to the below instead
@HiltViewModel
class MainViewModel @Inject constructor(
    //no explicit constructor for the mainRepository, but because dagger
    //knows how to make its requested parts (runDao), it can make this too
    //no need to write an explicit constructor!
    val mainRepository: MainRepository
): ViewModel() {
}