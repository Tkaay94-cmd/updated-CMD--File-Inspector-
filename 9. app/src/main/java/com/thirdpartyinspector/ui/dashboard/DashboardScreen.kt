kotlin
package com.thirdpartyinspector.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DashboardScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "ThirdPartyInspector")
        // TODO: Replace with actual summary widgets wired to ViewModel
        Button(onClick = { /* trigger scan */ }) {
            Text("Scan now")
        }
    }
}
