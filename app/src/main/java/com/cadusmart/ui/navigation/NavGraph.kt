package com.cadusmart.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // si no resuelve: androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cadusmart.domain.Role
import com.cadusmart.ui.MainViewModel
import com.cadusmart.ui.SessionState
import com.cadusmart.ui.alerts.AlertsScreen
import com.cadusmart.ui.auth.ChangePasswordScreen
import com.cadusmart.ui.auth.LoginScreen
import com.cadusmart.ui.auth.RegisterScreen
import com.cadusmart.ui.inventory.InventoryScreen
import com.cadusmart.ui.inventory.ProductDetailScreen
import com.cadusmart.ui.inventory.ProductFormScreen
import com.cadusmart.ui.inventory.TrashScreen
import com.cadusmart.ui.profile.ProfileScreen
import com.cadusmart.ui.scanner.ScannerScreen
import com.cadusmart.ui.users.UserFormScreen
import com.cadusmart.ui.users.UsersScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private fun tabsFor(role: Role): List<Tab> = buildList {
    add(Tab(Routes.INVENTORY, "Inventario", Icons.Outlined.Inventory2))
    add(Tab(Routes.ALERTS, "Alertas", Icons.Outlined.Notifications))
    if (role.canManageUsers) add(Tab(Routes.USERS, "Usuarios", Icons.Outlined.Group))
    else add(Tab(Routes.PROFILE, "Mi perfil", Icons.Outlined.Person))
}

@Composable
fun AppNavHost(vm: MainViewModel = hiltViewModel()) {
    val session by vm.session.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    // Mientras DataStore lee la sesión guardada: pantalla vacía (evita parpadeo de Login)
    if (session is SessionState.Loading) {
        Box(Modifier.fillMaxSize())
        return
    }

    val current = session

    // Contraseña temporal: no se puede usar la app hasta cambiarla
    if (current is SessionState.LoggedIn && current.mustChangePassword) {
        ChangePasswordScreen()
        return
    }

    val role = (current as? SessionState.LoggedIn)?.role
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = role?.let(::tabsFor).orEmpty()
    val showBar = tabs.any { it.route == currentRoute }

    // La sesión manda: al iniciar entra a la app; al cerrarla (o vencer el token) vuelve al Login
    LaunchedEffect(current, currentRoute) {
        val inAuth = currentRoute == Routes.LOGIN || currentRoute == Routes.REGISTER
        when (current) {
            is SessionState.LoggedIn ->
                if (inAuth) nav.navigate(Routes.INVENTORY) { popUpTo(0) { inclusive = true } }
            is SessionState.LoggedOut ->
                if (currentRoute != null && !inAuth)
                    nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            else -> Unit
        }
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (role != null) Routes.INVENTORY else Routes.LOGIN,
            modifier = Modifier.padding(padding)
        ) {
            // ---- Autenticación ----
            composable(Routes.LOGIN) {
                LoginScreen(onGoRegister = { nav.navigate(Routes.REGISTER) })
            }
            composable(Routes.REGISTER) {
                RegisterScreen(onBack = { nav.popBackStack() })
            }

            // ---- Pestañas ----
            composable(Routes.INVENTORY) {
                InventoryScreen(
                    onAdd = { nav.navigate(Routes.PRODUCT_NEW) },
                    onOpen = { id -> nav.navigate(Routes.productDetail(id)) },
                    onTrash = if (role?.canViewDeletedProducts == true) {
                        { nav.navigate(Routes.TRASH) }
                    } else null
                )
            }
            composable(Routes.ALERTS) {
                AlertsScreen(onOpen = { id -> nav.navigate(Routes.productDetail(id)) })
            }
            composable(Routes.PROFILE) { ProfileScreen() }
            composable(Routes.USERS) {
                UsersScreen(
                    onAdd = { nav.navigate(Routes.USER_NEW) },
                    onEdit = { id -> nav.navigate(Routes.userEdit(id)) }
                )
            }

            // ---- Inventario ----
            composable(Routes.PRODUCT_NEW) {
                ProductFormScreen(
                    onBack = { nav.popBackStack() },
                    onScan = { nav.navigate(Routes.SCANNER) }
                )
            }
            composable(Routes.SCANNER) {
                ScannerScreen(
                    onBack = { nav.popBackStack() },
                    onResult = { code ->
                        // Se entrega al formulario anterior (lo lee ProductFormViewModel)
                        nav.previousBackStackEntry?.savedStateHandle?.set("barcode", code)
                        nav.popBackStack()
                    }
                )
            }
            composable(Routes.PRODUCT_DETAIL) {
                ProductDetailScreen(
                    canDelete = role?.canDeleteProducts == true,
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.productEdit(id)) }
                )
            }
            composable(Routes.PRODUCT_EDIT) {
                ProductFormScreen(
                    onBack = { nav.popBackStack() },
                    onScan = { nav.navigate(Routes.SCANNER) }
                )
            }
            composable(Routes.TRASH) {
                TrashScreen(onBack = { nav.popBackStack() })
            }

            // ---- Usuarios (Admin) ----
            composable(Routes.USER_NEW) {
                UserFormScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.USER_EDIT) {
                UserFormScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goToTab(route: String) = navigate(route) {
    popUpTo(Routes.INVENTORY) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Pantalla temporal para probar el flujo. Se reemplaza pantalla por pantalla. */
@Composable
private fun Placeholder(title: String, vararg actions: Pair<String, () -> Unit>) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        actions.forEach { (label, onClick) ->
            Button(onClick = onClick) { Text(label) }
        }
    }
}
