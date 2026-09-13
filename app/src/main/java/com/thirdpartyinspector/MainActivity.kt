kotlin
package com.thirdpartyinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.thirdpartyinspector.ui.theme.ThirdPartyInspectorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThirdPartyInspectorTheme {
                // TODO: Replace with Navigation and Dashboard screen
                com.thirdpartyinspector.ui.dashboard.DashboardScreen()
            }
        }
    }
}
