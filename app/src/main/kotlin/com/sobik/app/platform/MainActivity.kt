package com.sobik.app.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.SobikApp

/** Keeps the back stack across configuration changes. */
class NavigatorHolder : ViewModel() {
    val navigator = Navigator()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SobikApplication).container
        setContent {
            val nav = viewModel<NavigatorHolder>().navigator
            BackHandler(enabled = nav.canGoBack) { nav.pop() }
            SobikApp(container, nav)
        }
    }
}
