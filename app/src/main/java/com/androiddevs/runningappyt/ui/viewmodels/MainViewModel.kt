package com.androiddevs.runningappyt.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.other.SortType
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

    private val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
    private val runsSortedByDistance = mainRepository.getAllRunsSortedByDistance()
    private val runsSortedByCaloriesBurned = mainRepository.getAllRunsSortedByCaloriesBurned()
    private val runsSortedByTimeInMillis = mainRepository.getAllRunsSortedByTimeInMillis()
    private val runsSortedByAvgSpeed = mainRepository.getAllRunsSortedByAvgSpeed()


    val runs = MediatorLiveData<List<Run>>()
    //mediator liveData allows for merging multiple liveDatas together

    var sortType = SortType.DATE

    //whenever liveData emits, observer is called and conducts
    init {
        //adds liveData as source and observes it
        runs.addSource(runsSortedByDate) { result ->
            if(sortType == SortType.DATE) {
                //if this sorting, let the runs livedata = the value of this result
                //so liveData gets bound to the new emitted changed data
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByAvgSpeed) { result ->
            if(sortType == SortType.AVG_SPEED) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByCaloriesBurned) { result ->
            if(sortType == SortType.CALORIES_BURNED) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByDistance) { result ->
            if(sortType == SortType.DISTANCE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByTimeInMillis) { result ->
            if(sortType == SortType.RUNNING_TIME) {
                result?.let { runs.value = it }
            }
        }
    }

    //similarly to above, on sort change affect the view's data with the emitted data
    fun sortRuns(sortType: SortType) = when(sortType) {
        SortType.DATE -> runsSortedByDate.value?.let {runs.value = it}
        SortType.AVG_SPEED -> runsSortedByAvgSpeed.value?.let {runs.value = it}
        SortType.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let {runs.value = it}
        SortType.DISTANCE -> runsSortedByDistance.value?.let {runs.value = it}
        SortType.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let {runs.value = it}
    }.also {
        //and mark the new sortType
        this.sortType = sortType
    }

    fun insertRun(run: Run) = viewModelScope.launch{
        mainRepository.insertRun(run)
    }
}