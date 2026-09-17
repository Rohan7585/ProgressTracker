package com.rohan.progresstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rohan.progresstracker.ui.AppNavigation
import com.rohan.progresstracker.ui.theme.ProgressTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProgressTrackerTheme {
                AppNavigation()
            }
        }
    }
}
