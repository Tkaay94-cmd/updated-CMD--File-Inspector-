package com.thirdpartyinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.thirdpartyinspector.ui.dashboard.DashboardScreen
import com.thirdpartyinspector.ui.dashboard.DashboardViewModel
import com.thirdpartyinspector.ui.theme.ThirdPartyInspectorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThirdPartyInspectorTheme {
                DashboardScreen(viewModel = dashboardViewModel)
            }
        }
    }
}
