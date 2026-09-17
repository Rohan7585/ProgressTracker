package com.rohan.progresstracker

import android.app.Application
import com.rohan.progresstracker.data.AppDatabase
import com.rohan.progresstracker.data.Repository

/** Creates the database and repository once, for the whole app. */
class ProgressTrackerApp : Application() {
    val repository: Repository by lazy { Repository(AppDatabase.create(this)) }
}
