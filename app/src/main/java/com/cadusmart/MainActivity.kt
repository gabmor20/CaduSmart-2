package com.cadusmart

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cadusmart.ui.navigation.AppNavHost
import com.cadusmart.ui.theme.CaduSmartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* resultado ignorado */ }

    // Se piden de a una: dos diálogos de permiso a la vez se cancelan entre sí
    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { askNotifications() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solo en debug: el backend de desarrollo está en una IP de red local.
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37) {
            requestLocalNetwork.launch("android.permission.ACCESS_LOCAL_NETWORK")
        } else {
            askNotifications()
        }

        setContent {
            CaduSmartTheme {
                AppNavHost()
            }
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
