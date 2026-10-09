package com.cadusmart.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrashUi(
    val loading: Boolean = true,
    val items: List<ProductDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repo: ProductRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(TrashUi())
    val ui: StateFlow<TrashUi> = _ui.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.value = TrashUi(loading = true)
            repo.trash()
                .onSuccess { _ui.value = TrashUi(loading = false, items = it) }
                .onFailure { _ui.value = TrashUi(loading = false, error = it.message) }
        }
    }

    fun purge(id: Int) {
        viewModelScope.launch {
            repo.purge(id)
                .onSuccess { load() }
                .onFailure { _ui.value = _ui.value.copy(error = it.message) }
        }
    }
}
