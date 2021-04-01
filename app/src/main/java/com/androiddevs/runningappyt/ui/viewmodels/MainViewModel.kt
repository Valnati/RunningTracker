package com.androiddevs.runningappyt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

//beta changed ViewModelInject to the below instead
@HiltViewModel
class MainViewModel @Inject constructor(
    //no explicit constructor for the mainRepository, but because dagger
    //knows how to make its requested parts (runDao), it can make this too
    //no need to write an explicit constructor!
    val mainRepository: MainRepository
): ViewModel() {

    fun insertRun(run: Run) = viewModelScope.launch{
        mainRepository.insertRun(run)
    }
}