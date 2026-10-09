package com.cadusmart.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cadusmart.data.api.AuthApi
import com.cadusmart.data.api.ProductApi
import com.cadusmart.data.dto.ProductDto
import com.cadusmart.data.repository.apiCall
import com.cadusmart.data.session.SessionStore
import com.cadusmart.domain.Product
import com.cadusmart.domain.buildExpiryAlert
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** Revisa el inventario del usuario y lanza un aviso si hay productos por vencer. */
@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authApi: AuthApi,
    private val productApi: ProductApi,
    private val session: SessionStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (session.token.first() == null) return Result.success()   // sin sesión: nada que avisar

        val user = apiCall { authApi.me().user }.getOrElse {
            // Si el token venció, el interceptor ya cerró la sesión
            return if (session.token.first() == null || runAttemptCount >= 3) Result.success()
            else Result.retry()
        }
        if (!user.notificationsEnabled) return Result.success()

        val products = apiCall { productApi.list().items }.getOrElse {
            return if (runAttemptCount >= 3) Result.success() else Result.retry()
        }

        val alert = buildExpiryAlert(
            userName = user.nombre.substringBefore(' '),
            products = products.map { it.toDomain() },
            alertDays = user.alertDays
        )
        if (alert != null) ExpiryNotifier.show(applicationContext, alert)
        return Result.success()
    }
}

private fun ProductDto.toDomain() = Product(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    lot = lot,
    barcode = barcode,
    expiryDate = LocalDate.parse(expiryDate),
    consumed = status == "CONSUMIDO"
)
