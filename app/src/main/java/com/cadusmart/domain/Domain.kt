package com.cadusmart.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class Role {
    PERSONAL, COMERCIO_ADMIN, COMERCIO_EMPLEADO;

    val canManageUsers get() = this == COMERCIO_ADMIN
    val canDeleteProducts get() = this != COMERCIO_EMPLEADO
    val canViewDeletedProducts get() = this == COMERCIO_ADMIN
    val canPurgeProducts get() = this == COMERCIO_ADMIN
    val canResetPasswords get() = this != PERSONAL      // admin: a otros; empleado: la suya
}

enum class ProductStatus(val label: String) {
    VENCIDO("Vencido"),
    POR_VENCER("Por vencer"),
    FRESCO("Fresco"),
    CONSUMIDO("Consumido")
}

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val quantity: Int,
    val lot: String?,
    val barcode: String?,
    val expiryDate: LocalDate,
    val consumed: Boolean = false,
    val deleted: Boolean = false
) {
    fun daysLeft(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, expiryDate)

    fun status(alertDays: Int, today: LocalDate = LocalDate.now()): ProductStatus {
        if (consumed) return ProductStatus.CONSUMIDO
        val left = daysLeft(today)
        return when {
            left <= 0 -> ProductStatus.VENCIDO           // "vence hoy" ya cuenta como vencido
            left <= alertDays -> ProductStatus.POR_VENCER
            else -> ProductStatus.FRESCO
        }
    }
}

/** Orden por urgencia para el listado. */
fun List<Product>.sortedByUrgency(alertDays: Int): List<Product> =
    filter { !it.deleted }
        .sortedWith(compareBy({ it.status(alertDays).ordinal }, { it.expiryDate }))

/** Contadores del encabezado del inventario. */
fun List<Product>.countBy(status: ProductStatus, alertDays: Int) =
    count { !it.deleted && it.status(alertDays) == status }

data class ExpiryAlert(
    val title: String,
    val summary: String,
    val details: List<String>   // un renglón por producto con su fecha límite
)

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Alerta individual del usuario: productos POR_VENCER según sus días de aviso
 * (por defecto 2). Devuelve null si no hay nada que avisar.
 */
fun buildExpiryAlert(
    userName: String,
    products: List<Product>,
    alertDays: Int = 2,
    today: LocalDate = LocalDate.now()
): ExpiryAlert? {
    val soon = products
        .filter { !it.deleted && it.status(alertDays, today) == ProductStatus.POR_VENCER }
        .sortedBy { it.expiryDate }
    if (soon.isEmpty()) return null

    val n = soon.size
    return ExpiryAlert(
        title = "Atención $userName: tienes $n producto${if (n == 1) "" else "s"} por vencer",
        summary = "Fecha límite más próxima: ${soon.first().expiryDate.format(dateFmt)}. Toca para revisarlos.",
        details = soon.map { "${it.name} · vence el ${it.expiryDate.format(dateFmt)}" }
    )
}

/** Mensaje del estado vacío según el filtro activo. */
fun emptyMessage(filter: ProductStatus?): String = when (filter) {
    null -> "Aún no has registrado ningún producto"
    else -> "No hay productos en estado ${filter.label.lowercase()}"
}