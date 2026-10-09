package com.cadusmart.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // si no resuelve: androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadusmart.data.dto.UserDto

fun roleLabel(role: String) = when (role) {
    "PERSONAL" -> "Personal"
    "COMERCIO_ADMIN" -> "Admin Comercio"
    "COMERCIO_EMPLEADO" -> "Empleado Comercio"
    else -> role
}

private fun initials(name: String) =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** [showHeader] = false cuando va dentro de la pestaña "Mi perfil" del Admin (ya tiene su propia cabecera). */
@Composable
fun ProfileScreen(showHeader: Boolean = true, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (showHeader) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "Mi perfil",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.user == null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error ?: "No se pudo cargar tu perfil", color = MaterialTheme.colorScheme.error)
                Button(onClick = vm::load) { Text("Reintentar") }
            }
            else -> ProfileContent(
                user = state.user!!,
                onLogout = { confirmLogout = true }
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que iniciar sesión otra vez para ver tu inventario.") },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } },
            confirmButton = {
                Button(
                    onClick = { confirmLogout = false; vm.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar sesión") }
            }
        )
    }
}

@Composable
private fun ProfileContent(user: UserDto, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials(user.nombre),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(user.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(user.email, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Rol: ${roleLabel(user.role)}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            ProfileRow("Editar mis datos", "")
            HorizontalDivider()
            ProfileRow("Días de aviso", "${user.alertDays} días")
            HorizontalDivider()
            ProfileRow("Notificaciones", if (user.notificationsEnabled) "Activas" else "Desactivadas")
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Cerrar sesión") }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
