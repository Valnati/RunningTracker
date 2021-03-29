package com.androiddevs.runningappyt.di

import android.content.Context
import androidx.room.Room
import com.androiddevs.runningappyt.db.RunDatabase
import com.androiddevs.runningappyt.other.Constants.RUNNING_DATABASE_NAME
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
}