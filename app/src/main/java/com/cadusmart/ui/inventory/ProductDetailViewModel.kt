package com.cadusmart.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUi(val working: Boolean = false, val error: String? = null, val finished: Boolean = false)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProductRepository
) : ViewModel() {

    private val id: Int = savedStateHandle.get<String>("id")?.toIntOrNull() ?: -1

    val product: StateFlow<ProductDto?> = repo.products
        .map { list -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(DetailUi())
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { if (!repo.loaded.value) repo.refresh() }
    }

    fun consume() = perform { repo.consume(id).map { } }

    /** Borrado lógico: al terminar, la pantalla se cierra */
    fun delete() = perform(finishOnSuccess = true) { repo.delete(id) }

    private fun perform(finishOnSuccess: Boolean = false, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _ui.value = DetailUi(working = true)
            val result = block()
            _ui.value = DetailUi(
                error = result.exceptionOrNull()?.message,
                finished = finishOnSuccess && result.isSuccess
            )
        }
    }
}
