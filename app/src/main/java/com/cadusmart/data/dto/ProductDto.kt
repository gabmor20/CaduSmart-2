package com.cadusmart.data.dto

data class ProductDto(
    val id: Int,
    val code: String,              // P-0142
    val name: String,
    val category: String,
    val quantity: Int,
    val lot: String?,
    val barcode: String?,
    val expiryDate: String,        // yyyy-MM-dd
    val daysLeft: Int,
    val status: String,            // VENCIDO | POR_VENCER | FRESCO | CONSUMIDO (lo calcula el servidor)
    val registeredAt: String,      // yyyy-MM-dd
    val deletedAt: String?
)

data class ProductsResponse(val items: List<ProductDto>)
data class ProductResponse(val item: ProductDto)
data class OkResponse(val ok: Boolean)

data class ProductRequest(
    val name: String,
    val category: String,
    val quantity: Int,
    val lot: String?,
    val barcode: String?,
    val expiryDate: String         // yyyy-MM-dd
)
