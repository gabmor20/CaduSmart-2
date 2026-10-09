package com.cadusmart

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.cadusmart.ui.navigation.AppNavHost
import com.cadusmart.ui.theme.CaduSmartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* resultado ignorado */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solo en debug: el backend de desarrollo está en una IP de red local.
        // En release el backend será una URL pública https y no hace falta.
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37) {
            requestLocalNetwork.launch("android.permission.ACCESS_LOCAL_NETWORK")
        }

        setContent {
            CaduSmartTheme {
                AppNavHost()
            }
        }
    }
}