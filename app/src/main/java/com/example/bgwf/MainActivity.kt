package com.example.bgwf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.bgwf.ui.MainScreen
import com.example.bgwf.utils.SharedPreferencesHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = remember { SharedPreferencesHelper(this) }

            MainScreen(
                sharedPreferencesHelper = prefs
            )
        }
    }
}