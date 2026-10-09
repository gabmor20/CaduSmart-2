package com.cadusmart.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // si no resuelve: androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.domain.Role
import com.cadusmart.ui.components.GreenHeader

/** Sirve para "Agregar producto" (sin id) y "Editar producto" (con id). */
@Composable
fun ProductFormScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    vm: ProductFormViewModel = hiltViewModel()
) {
    val product by vm.product.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val role by vm.role.collectAsStateWithLifecycle()
    val editing = vm.editingId != null

    LaunchedEffect(ui.saved) { if (ui.saved) onBack() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GreenHeader(
            title = if (editing) "Editar producto" else "Agregar producto",
            subtitle = product?.code,
            onBack = onBack
        )
        if (editing && product == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            FormContent(
                initial = product,
                role = role,
                editing = editing,
                saving = ui.saving,
                error = ui.error,
                onScan = onScan,
                onChange = vm::clearError,
                onCancel = onBack,
                onSave = vm::save
            )
        }
    }
}

@Composable
private fun FormContent(
    initial: ProductDto?,
    role: Role?,
    editing: Boolean,
    saving: Boolean,
    error: String?,
    onScan: () -> Unit,
    onChange: () -> Unit,
    onCancel: () -> Unit,
    onSave: (name: String, category: String, quantity: String, lot: String, barcode: String, date: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: "") }
    var quantity by rememberSaveable { mutableStateOf(initial?.quantity?.toString() ?: "1") }
    var lot by rememberSaveable { mutableStateOf(initial?.lot ?: "") }
    var barcode by rememberSaveable { mutableStateOf(initial?.barcode ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.expiryDate?.let(::formatDate) ?: "") }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!editing) {
            OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Escanear código de barras")
            }
            Text("o escribe los datos a mano", style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onChange() },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CategoryField(
            value = category,
            options = categoriesFor(role, initial?.category),
            onSelect = { category = it; onChange() }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter(Char::isDigit).take(4); onChange() },
                label = { Text("Cantidad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lot,
                onValueChange = { lot = it; onChange() },
                label = { Text("Lote") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it.filter(Char::isDigit); onChange() },
            label = { Text("Código de barras (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = date,
            onValueChange = { date = autoSlash(it); onChange() },
            label = { Text("Fecha de vencimiento") },
            placeholder = { Text("dd/mm/aaaa") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (editing && initial != null) {
            Text(
                "Estado actual: ${initial.statusEnum().label}. El estado se recalcula solo si cambias la fecha.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("Cancelar") }
            Button(
                onClick = { onSave(name, category, quantity, lot, barcode, date) },
                enabled = !saving,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                if (saving) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                ) else Text(if (editing) "Guardar cambios" else "Guardar producto")
            }
        }
    }
}

/** Escribe solo números y pone las "/" solas: 22092026 -> 22/09/2026 */
private fun autoSlash(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('/')
            append(c)
        }
    }
}

@Composable
private fun CategoryField(value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            placeholder = { Text("Selecciona") },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente que abre el menú al tocar cualquier parte del campo
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
