package com.cadusmart.data.repository

import com.cadusmart.data.api.ProductApi
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.data.dto.ProductRequest
import com.cadusmart.data.session.SessionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Fuente única del inventario: todas las pantallas leen [products] y cada cambio
 * (crear, editar, consumir, eliminar) refresca la lista.
 */
@Singleton
class ProductRepository @Inject constructor(
    private val api: ProductApi,
    session: SessionStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _products = MutableStateFlow<List<ProductDto>>(emptyList())
    val products: StateFlow<List<ProductDto>> = _products.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        // Al cerrar sesión se vacía la caché para no mostrar datos de otro usuario
        scope.launch {
            session.token.map { it != null }.distinctUntilChanged().collect { hasSession ->
                if (!hasSession) {
                    _products.value = emptyList()
                    _loaded.value = false
                }
            }
        }
    }

    suspend fun refresh(): Result<Unit> = apiCall {
        _products.value = api.list().items
        _loaded.value = true
        Unit
    }

    suspend fun trash(): Result<List<ProductDto>> = apiCall { api.trash().items }

    suspend fun create(request: ProductRequest): Result<ProductDto> =
        apiCall { api.create(request).item }.also { if (it.isSuccess) refresh() }

    suspend fun update(id: Int, request: ProductRequest): Result<ProductDto> =
        apiCall { api.update(id, request).item }.also { if (it.isSuccess) refresh() }

    suspend fun consume(id: Int): Result<ProductDto> =
        apiCall { api.consume(id).item }.also { if (it.isSuccess) refresh() }

    /** Borrado lógico */
    suspend fun delete(id: Int): Result<Unit> =
        apiCall { api.delete(id); Unit }.also { if (it.isSuccess) refresh() }

    /** Eliminación definitiva desde la papelera (solo Admin) */
    suspend fun purge(id: Int): Result<Unit> = apiCall { api.purge(id); Unit }
}
