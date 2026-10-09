package com.cadusmart.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.data.repository.ProductRepository
import com.cadusmart.domain.ProductStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repo: ProductRepository
) : ViewModel() {

    val products: StateFlow<List<ProductDto>> = repo.products

    private val _filter = MutableStateFlow<ProductStatus?>(null)   // null = Todos
    val filter: StateFlow<ProductStatus?> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = repo.refresh().exceptionOrNull()?.message
            _loading.value = false
        }
    }

    fun setFilter(status: ProductStatus?) { _filter.value = status }
    fun setQuery(text: String) { _query.value = text }
}
