package com.cadusmart.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Pestañas principales
    const val INVENTORY = "inventory"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"   // Personal y Empleado
    const val USERS = "users"       // Admin Comercio (incluye "Mi perfil")

    // Inventario
    const val PRODUCT_NEW = "product_new"
    const val SCANNER = "com/cadusmart/ui/scanner"
    const val PRODUCT_DETAIL = "product/{id}"
    const val PRODUCT_EDIT = "product_edit/{id}"
    const val TRASH = "trash"       // papelera: solo Admin

    // Usuarios (Admin)
    const val USER_NEW = "user_new"
    const val USER_EDIT = "user_edit/{id}"

    fun productDetail(id: Int) = "product/$id"
    fun productEdit(id: Int) = "product_edit/$id"
    fun userEdit(id: Int) = "user_edit/$id"
}