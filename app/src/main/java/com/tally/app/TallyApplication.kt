package com.tally.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Entry point for Hilt dependency injection
// TallyApplication is the base Application class that Hilt uses
@HiltAndroidApp
class TallyApplication : Application()
