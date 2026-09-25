package ar.net.dahool.upsmonitor.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.net.dahool.upsmonitor.UiState
import ar.net.dahool.upsmonitor.data.model.StatusEvent
import ar.net.dahool.upsmonitor.data.model.UpsMetrics
import ar.net.dahool.upsmonitor.ui.components.InfoRow
import ar.net.dahool.upsmonitor.ui.components.MetricCard
import ar.net.dahool.upsmonitor.ui.components.StatusBadge
import ar.net.dahool.upsmonitor.ui.theme.UpsColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: UiState,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isRefreshing = uiState is UiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "UPS Monitor",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        if (uiState is UiState.Success) {
                            val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            Text(
                                "Updated ${fmt.format(Date(uiState.lastUpdated))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(150))
                },
                label = "screenState"
            ) { state ->
                when (state) {
                    is UiState.Loading -> LoadingContent()
                    is UiState.Success -> DashboardContent(state.metrics, state.history)
                    is UiState.Error -> ErrorContent(state.message, onRefresh)
                    is UiState.Unconfigured -> UnconfiguredContent(onSettingsClick)
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(m: UpsMetrics, history: List<StatusEvent> = emptyList()) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device identity header
        DeviceHeader(m)

        val normalizedStatus = when {
            m.status.startsWith("OL", ignoreCase = true) -> "Online"
            m.status.startsWith("LB", ignoreCase = true) -> "Low Battery"
            m.status.startsWith("OB", ignoreCase = true) -> "Battery"
            else -> m.status // fallback to original text
        }

        StatusBadge(
            status = normalizedStatus,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Primary metrics grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val batteryColor = when {
                m.batteryChargeInt < 20 -> UpsColors.critical
                m.batteryChargeInt < 50 -> UpsColors.battery
                else -> UpsColors.online
            }
            MetricCard(
                label = "Battery",
                value = m.batteryCharge,
                unit = "%",
                accentColor = batteryColor,
                progressFraction = m.batteryChargeInt / 100f,
                progressColor = batteryColor,
                modifier = Modifier.weight(1f)
            )
            val loadColor = when {
                m.upsLoadInt > 80 -> UpsColors.critical
                m.upsLoadInt > 60 -> UpsColors.battery
                else -> UpsColors.value
            }
            MetricCard(
                label = "Load",
                value = m.upsLoad,
                unit = "%",
                accentColor = loadColor,
                progressFraction = m.upsLoadInt / 100f,
                progressColor = loadColor,
                modifier = Modifier.weight(1f)
            )
        }

        MetricCard(
            label = "Runtime Remaining",
            value = m.runtimeFormatted,
            accentColor = if (m.runtimeSeconds.toIntOrNull() ?: 999 < 900)
                UpsColors.critical else UpsColors.online,
            modifier = Modifier.fillMaxWidth()
        )

        if (m.lastIncident != null) {
            MetricCard(
                label = "Last Incident",
                value = m.lastIncident.getFormattedChangedAt(context),
                accentColor = UpsColors.battery,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(label = "Power Input", icon = Icons.Outlined.ElectricalServices) {
            InfoRow("Input Voltage", "${m.inputVoltage} V")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            InfoRow("Nominal Input", "${m.inputVoltageNominal} V")
        }

        SectionCard(label = "Battery", icon = Icons.Outlined.BatteryAlert) {
            InfoRow("Voltage", "${m.batteryVoltage} V")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            InfoRow("Nominal Voltage", "${m.batteryVoltageNominal} V")
        }

        if (history.isNotEmpty()) {
            SectionCard(label = "Event History", icon = Icons.Outlined.History) {
                history.forEachIndexed { index, event ->
                    InfoRow(event.getFormattedChangedAt(context), event.status)
                    if (index < history.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }

        SectionCard(label = "Device Info", icon = Icons.Outlined.Info) {
            InfoRow("Manufacturer", m.manufacturer)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            InfoRow("Model", m.model)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            InfoRow("Serial", m.serial)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DeviceHeader(m: UpsMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = m.model,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.W600,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = m.manufacturer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    label: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ElectricalServices,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = UpsColors.critical
            )
            Text(
                text = "Can't reach server",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W600),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun UnconfiguredContent(onSettingsClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Server not configured",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W600),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Set your UPS monitor server URL to start.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onSettingsClick) {
                Text("Open Settings")
            }
        }
    }
}
