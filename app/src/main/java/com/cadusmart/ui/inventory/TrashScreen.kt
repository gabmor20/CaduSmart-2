package com.cadusmart.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // si no resuelve: androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.ui.components.GreenHeader

/** Papelera: productos con borrado lógico. Solo la ve el Admin Comercio. */
@Composable
fun TrashScreen(onBack: () -> Unit, vm: TrashViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var toPurge by remember { mutableStateOf<ProductDto?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GreenHeader(
            title = "Papelera",
            subtitle = "Productos eliminados",
            onBack = onBack
        )
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.items.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(ui.error ?: "La papelera está vacía", color = if (ui.error != null) MaterialTheme.colorScheme.error else Color.Unspecified)
                if (ui.error != null) Button(onClick = vm::load) { Text("Reintentar") }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ui.error != null) item { Text(ui.error!!, color = MaterialTheme.colorScheme.error) }
                items(ui.items, key = { it.id }) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${p.category} · ${p.code}" + (p.lot?.let { " · lote $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Vencía el ${formatDate(p.expiryDate)} · eliminado el ${p.deletedAt?.let(::formatDate) ?: "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = { toPurge = p },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Eliminar definitivamente") }
                        }
                    }
                }
            }
        }
    }

    toPurge?.let { p ->
        AlertDialog(
            onDismissRequest = { toPurge = null },
            title = { Text("¿Eliminar definitivamente?") },
            text = { Text("Se borra \"${p.name}\" de forma permanente. Esta acción no se puede deshacer.") },
            dismissButton = { TextButton(onClick = { toPurge = null }) { Text("Cancelar") } },
            confirmButton = {
                Button(
                    onClick = { vm.purge(p.id); toPurge = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            }
        )
    }
}
