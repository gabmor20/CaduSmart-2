package com.cadusmart.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cadusmart.domain.Role
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("token")
    private val roleKey = stringPreferencesKey("role")

    val token: Flow<String?> = context.sessionDataStore.data.map { it[tokenKey] }

    /** null = no hay sesión */
    val role: Flow<Role?> = context.sessionDataStore.data.map { prefs ->
        if (prefs[tokenKey] == null) null
        else prefs[roleKey]?.let { runCatching { Role.valueOf(it) }.getOrNull() }
    }

    suspend fun save(token: String, role: Role) {
        context.sessionDataStore.edit {
            it[tokenKey] = token
            it[roleKey] = role.name
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
