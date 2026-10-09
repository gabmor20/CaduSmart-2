package com.cadusmart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Verde = Color(0xFF1E6B4B)
val VerdeOscuro = Color(0xFF0F3D2B)
val VerdeClaro = Color(0xFFDCEBE3)
val Naranja = Color(0xFFF59B4A)
val Rojo = Color(0xFFB3261E)
val Fondo = Color(0xFFF3F5F3)

private val Colors = lightColorScheme(
    primary = Verde,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = VerdeOscuro,
    secondary = Naranja,
    onSecondary = Color.White,
    error = Rojo,
    background = Fondo,
    surface = Color.White
)

@Composable
fun CaduSmartTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}