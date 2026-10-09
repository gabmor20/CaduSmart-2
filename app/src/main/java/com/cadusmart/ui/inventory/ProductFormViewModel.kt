package com.cadusmart.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.data.dto.ProductRequest
import com.cadusmart.data.repository.ProductRepository
import com.cadusmart.data.session.SessionStore
import com.cadusmart.domain.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormUi(val saving: Boolean = false, val error: String? = null, val saved: Boolean = false)

private val DATE_IN = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProductRepository,
    sessionStore: SessionStore
) : ViewModel() {

    /** null = estamos creando un producto nuevo */
    val editingId: Int? = savedStateHandle.get<String>("id")?.toIntOrNull()

    val product: StateFlow<ProductDto?> = repo.products
        .map { list -> list.firstOrNull { it.id == editingId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val role: StateFlow<Role?> = sessionStore.role
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(FormUi())
    val ui: StateFlow<FormUi> = _ui.asStateFlow()

    init {
        if (editingId != null) viewModelScope.launch { if (!repo.loaded.value) repo.refresh() }
    }

    fun clearError() {
        if (_ui.value.error != null) _ui.update { it.copy(error = null) }
    }

    fun save(name: String, category: String, quantity: String, lot: String, barcode: String, date: String) {
        val qty = quantity.trim().toIntOrNull()
        val expiry = runCatching { LocalDate.parse(date.trim(), DATE_IN) }.getOrNull()
        val error = when {
            name.isBlank() -> "Escribe el nombre del producto"
            category.isBlank() -> "Selecciona una categoría"
            qty == null || qty < 1 -> "La cantidad debe ser un número mayor a 0"
            expiry == null -> "Escribe la fecha de vencimiento con el formato dd/mm/aaaa"
            else -> null
        }
        if (error != null || qty == null || expiry == null) {
            _ui.value = FormUi(error = error)
            return
        }

        val request = ProductRequest(
            name = name.trim(),
            category = category.trim(),
            quantity = qty,
            lot = lot.trim().ifBlank { null },
            barcode = barcode.trim().ifBlank { null },
            expiryDate = expiry.toString()
        )
        viewModelScope.launch {
            _ui.value = FormUi(saving = true)
            val id = editingId
            val result = if (id == null) repo.create(request) else repo.update(id, request)
            _ui.value = FormUi(
                error = result.exceptionOrNull()?.message,
                saved = result.isSuccess
            )
        }
    }
}
