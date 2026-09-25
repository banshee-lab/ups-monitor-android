package ar.net.dahool.upsmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ar.net.dahool.upsmonitor.ui.screens.DashboardScreen
import ar.net.dahool.upsmonitor.ui.screens.SettingsScreen
import ar.net.dahool.upsmonitor.ui.theme.UpsMonitorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: UpsViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            UpsMonitorTheme {
                AppNavigation(viewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun AppNavigation(viewModel: UpsViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()

    // Manage foreground polling lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refresh()
                    viewModel.startPolling()
                    viewModel.onFcmTokenRefreshed(force = false)
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showSettings) {
        SettingsScreen(
            currentUrl = serverUrl,
            onSave = { viewModel.saveServerUrl(it) },
            onBack = { showSettings = false }
        )
    } else {
        DashboardScreen(
            uiState = uiState,
            onRefresh = { viewModel.refresh() },
            onSettingsClick = { showSettings = true }
        )
    }
}
