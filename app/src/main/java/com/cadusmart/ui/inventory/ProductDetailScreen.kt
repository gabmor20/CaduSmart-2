package com.cadusmart.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
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

@Composable
fun ProductDetailScreen(
    canDelete: Boolean,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    vm: ProductDetailViewModel = hiltViewModel()
) {
    val product by vm.product.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmConsume by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(ui.finished) { if (ui.finished) onBack() }

    val p = product
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GreenHeader(
            title = p?.name ?: "Producto",
            onBack = onBack,
            actions = {
                if (canDelete && p != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )

        if (p == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            DetailBody(
                p = p,
                working = ui.working,
                error = ui.error,
                onConsume = { confirmConsume = true },
                onEdit = { onEdit(p.id) }
            )
        }
    }

    if (confirmConsume) {
        AlertDialog(
            onDismissRequest = { confirmConsume = false },
            title = { Text("¿Marcar como consumido?") },
            text = { Text("Al marcar el consumo de este producto se eliminará de la pantalla principal.") },
            dismissButton = { TextButton(onClick = { confirmConsume = false }) { Text("Cancelar") } },
            confirmButton = {
                Button(onClick = { confirmConsume = false; vm.consume() }) { Text("Confirmar") }
            }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este producto?") },
            text = {
                Text(
                    "Sale del inventario y de las estadísticas. " +
                        "Si el producto se consumió o se vendió, usa Marcar consumido en vez de esto."
                )
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; vm.delete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            }
        )
    }
}

@Composable
private fun DetailBody(
    p: ProductDto,
    working: Boolean,
    error: String?,
    onConsume: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StatusBadge(p.statusEnum())
                Text(remainingText(p), style = MaterialTheme.typography.titleMedium)
                Text("Vence el ${formatDate(p.expiryDate)}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                InfoRow("ID", p.code)
                HorizontalDivider()
                InfoRow("Categoría", p.category)
                HorizontalDivider()
                InfoRow("Lote", p.lot ?: "—")
                HorizontalDivider()
                InfoRow("Cantidad", "${p.quantity} ${if (p.quantity == 1) "unidad" else "unidades"}")
                HorizontalDivider()
                InfoRow("Código de barras", p.barcode ?: "—")
                HorizontalDivider()
                InfoRow("Registrado", formatDate(p.registeredAt))
            }
        }

        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)

        if (p.status != "CONSUMIDO") {
            Button(
                onClick = onConsume,
                enabled = !working,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Marcar consumido") }
            OutlinedButton(
                onClick = onEdit,
                enabled = !working,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Editar") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
