package com.thirdpartyinspector.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun DashboardScreen() {
    Column(modifier = Modifier.fillMaxSize().testTag("dashboard-screen")) {
        Text(text = "ThirdPartyInspector", modifier = Modifier.testTag("dashboard-title"))
        // TODO: Replace with actual summary widgets wired to ViewModel
        Button(onClick = { /* trigger scan */ }, modifier = Modifier.testTag("dashboard-scan-button")) {
            Text("Scan now")
        }
    }
}
