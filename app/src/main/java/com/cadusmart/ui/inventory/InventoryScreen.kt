package com.cadusmart.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // si no resuelve: androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.domain.ProductStatus
import com.cadusmart.domain.emptyMessage
import com.cadusmart.ui.components.GreenHeader

private val FILTERS: List<Pair<ProductStatus?, String>> = listOf(
    null to "Todos",
    ProductStatus.POR_VENCER to "Por vencer",
    ProductStatus.VENCIDO to "Vencidos",
    ProductStatus.CONSUMIDO to "Consumido"
)

@Composable
fun InventoryScreen(
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
    onTrash: (() -> Unit)?,          // solo Admin
    vm: InventoryViewModel = hiltViewModel()
) {
    val products by vm.products.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    // "Todos" no incluye los consumidos: salen de la pantalla principal y viven en su filtro
    val active = products.filter { it.status != ProductStatus.CONSUMIDO.name }
    val porVencer = active.count { it.status == ProductStatus.POR_VENCER.name }
    val vencidos = active.count { it.status == ProductStatus.VENCIDO.name }
    val frescos = active.count { it.status == ProductStatus.FRESCO.name }

    val currentFilter = filter
    val base = if (currentFilter == null) active else products.filter { it.status == currentFilter.name }
    val visible = base.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true) ||
            (it.lot?.contains(query, ignoreCase = true) == true)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            GreenHeader(
                title = "Mi inventario",
                subtitle = "${active.size} ${if (active.size == 1) "producto" else "productos"}",
                actions = {
                    if (onTrash != null) {
                        IconButton(onClick = onTrash) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Papelera",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )

            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Buscar producto") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CounterCard("Por vencer", porVencer, statusColor(ProductStatus.POR_VENCER), Modifier.weight(1f))
                    CounterCard("Vencidos", vencidos, statusColor(ProductStatus.VENCIDO), Modifier.weight(1f))
                    CounterCard("Frescos", frescos, statusColor(ProductStatus.FRESCO), Modifier.weight(1f))
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FILTERS.forEach { (status, label) ->
                        FilterChip(
                            selected = currentFilter == status,
                            onClick = { vm.setFilter(status) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            when {
                loading && products.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && products.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    Button(onClick = vm::refresh) { Text("Reintentar") }
                }
                visible.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (query.isNotBlank()) "No hay resultados para \"$query\"" else emptyMessage(currentFilter),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.id }) { p -> ProductCard(p, onClick = { onOpen(p.id) }) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Agregar") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Composable
private fun CounterCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ProductCard(p: ProductDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${p.category} · ${dueText(p)}" + (p.lot?.let { " · lote $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            StatusBadge(p.statusEnum())
        }
    }
}
