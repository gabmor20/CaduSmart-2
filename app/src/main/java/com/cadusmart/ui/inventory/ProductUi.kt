package com.cadusmart.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.domain.ProductStatus
import com.cadusmart.domain.Role
import com.cadusmart.ui.theme.Naranja
import com.cadusmart.ui.theme.Rojo
import com.cadusmart.ui.theme.Verde

fun ProductDto.statusEnum(): ProductStatus =
    runCatching { ProductStatus.valueOf(status) }.getOrDefault(ProductStatus.FRESCO)

/** yyyy-MM-dd -> dd/MM/yyyy */
fun formatDate(iso: String): String =
    iso.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0]}" else iso }

fun dueText(p: ProductDto): String = when {
    p.status == "CONSUMIDO" -> "consumido"
    p.daysLeft < 0 -> "vencido hace ${-p.daysLeft} ${if (p.daysLeft == -1) "día" else "días"}"
    p.daysLeft == 0 -> "vence hoy"
    p.daysLeft == 1 -> "vence mañana"
    else -> "en ${p.daysLeft} días"
}

fun remainingText(p: ProductDto): String = when {
    p.status == "CONSUMIDO" -> "Producto consumido"
    p.daysLeft < 0 -> "Venció hace ${-p.daysLeft} ${if (p.daysLeft == -1) "día" else "días"}"
    p.daysLeft == 0 -> "Vence hoy"
    p.daysLeft == 1 -> "Falta 1 día"
    else -> "Faltan ${p.daysLeft} días"
}

fun categoriesFor(role: Role?, current: String? = null): List<String> {
    val base = listOf(
        "Lácteos", "Granos", "Carnes y embutidos", "Frutas y verduras",
        "Panadería", "Bebidas", "Enlatados", "Congelados"
    )
    val extra = if (role == null || role == Role.PERSONAL) listOf("Otros")
    else listOf("Medicamentos", "Aseo y hogar", "Otros")
    val all = base + extra
    return if (current != null && current !in all) all + current else all
}

fun statusColor(status: ProductStatus): Color = when (status) {
    ProductStatus.VENCIDO -> Rojo
    ProductStatus.POR_VENCER -> Naranja
    ProductStatus.FRESCO -> Verde
    ProductStatus.CONSUMIDO -> Color(0xFF6B7280)
}

@Composable
fun StatusBadge(status: ProductStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
