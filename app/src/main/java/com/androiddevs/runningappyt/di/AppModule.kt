package com.androiddevs.runningappyt.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.androiddevs.runningappyt.db.RunDatabase
import com.androiddevs.runningappyt.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.androiddevs.runningappyt.other.Constants.KEY_NAME
import com.androiddevs.runningappyt.other.Constants.KEY_WEIGHT
import com.androiddevs.runningappyt.other.Constants.RUNNING_DATABASE_NAME
import com.androiddevs.runningappyt.other.Constants.SHARED_PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//singleton has its own package inside components; all others live in components itself
@Module
@InstallIn(SingletonComponent::class)
//annotations add components that determine when module is inside correct class,
//and when to destroy them
//singleton will be added to lifetime of ap, destroyed on app destroyed
object AppModule {
    //dagger handles onCreate and other overrides

    @Singleton //forces this to be a singleston (not sure needed with SingletonComponent?)
    @Provides //indicated result can be injected to other classes, used in other dependencies
    fun provideRunningDatabase(
        //define where the context is coming from
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        //get context from database function
        app,
        RunDatabase::class.java,
        RUNNING_DATABASE_NAME
    ).build()

    //now provide the dao object (by implementing class' abstract function
    @Singleton
    @Provides
    fun provideRunDao(db: RunDatabase) = db.getRunDao()
    //dagger can pass the class itself, and any defined functions
    //this is the injection part - dagger uses the provide function
    //to build it automatically as needed

    //
    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    //sometimes getString returns null so gotta have a nullcheck (it shouldn't but does)
    fun provideName(sharedPref: SharedPreferences) = sharedPref.getString(KEY_NAME, "") ?: ""

    @Singleton
    @Provides
    fun provideWeight(sharedPref: SharedPreferences) = sharedPref.getFloat(KEY_WEIGHT, 80f)

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharedPref: SharedPreferences) =
        sharedPref.getBoolean(KEY_FIRST_TIME_TOGGLE, true)
}