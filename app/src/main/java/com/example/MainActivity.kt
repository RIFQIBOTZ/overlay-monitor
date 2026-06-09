package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

private const val PREFS_NAME = "overlay_monitor_prefs"
private const val KEY_BATTERY_TEMP = "show_battery_temp"
private const val KEY_RAM_USAGE = "show_ram_usage"
private const val KEY_CPU_TEMP = "show_cpu_temp"
private const val KEY_CPU_USAGE = "show_cpu_usage"

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val isPermissionGranted = mutableStateOf(false)
    private val isServiceRunning = mutableStateOf(false)
    private val showBatteryTemp = mutableStateOf(true)
    private val showRamUsage = mutableStateOf(true)
    private val showCpuTemp = mutableStateOf(true)
    private val showCpuUsage = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read saved setting values (default to true)
        showBatteryTemp.value = prefs.getBoolean(KEY_BATTERY_TEMP, true)
        showRamUsage.value = prefs.getBoolean(KEY_RAM_USAGE, true)
        showCpuTemp.value = prefs.getBoolean(KEY_CPU_TEMP, true)
        showCpuUsage.value = prefs.getBoolean(KEY_CPU_USAGE, true)

        // Initialize the centralized Telemetry Engine here so the preview is alive!
        TelemetryEngine.start(this)

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        isPermissionGranted = isPermissionGranted.value,
                        isServiceRunning = isServiceRunning.value,
                        showBatteryTemp = showBatteryTemp.value,
                        showRamUsage = showRamUsage.value,
                        showCpuTemp = showCpuTemp.value,
                        showCpuUsage = showCpuUsage.value,
                        onToggleBattery = { checked ->
                            showBatteryTemp.value = checked
                            prefs.edit().putBoolean(KEY_BATTERY_TEMP, checked).apply()
                        },
                        onToggleRam = { checked ->
                            showRamUsage.value = checked
                            prefs.edit().putBoolean(KEY_RAM_USAGE, checked).apply()
                        },
                        onToggleCpuTemp = { checked ->
                            showCpuTemp.value = checked
                            prefs.edit().putBoolean(KEY_CPU_TEMP, checked).apply()
                        },
                        onToggleCpuUsage = { checked ->
                            showCpuUsage.value = checked
                            prefs.edit().putBoolean(KEY_CPU_USAGE, checked).apply()
                        },
                        onRequestPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        },
                        onStartService = {
                            startService(Intent(this, FloatingService::class.java))
                            isServiceRunning.value = true
                        },
                        onStopService = {
                            stopService(Intent(this, FloatingService::class.java))
                            isServiceRunning.value = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        checkServiceStatus()
    }

    private fun checkOverlayPermission() {
        isPermissionGranted.value = Settings.canDrawOverlays(this)
    }

    private fun checkServiceStatus() {
        isServiceRunning.value = FloatingService.isRunning
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isPermissionGranted: Boolean,
    isServiceRunning: Boolean,
    showBatteryTemp: Boolean,
    showRamUsage: Boolean,
    showCpuTemp: Boolean,
    showCpuUsage: Boolean,
    onToggleBattery: (Boolean) -> Unit,
    onToggleRam: (Boolean) -> Unit,
    onToggleCpuTemp: (Boolean) -> Unit,
    onToggleCpuUsage: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Material 3 Toolbar Look (Professional Polish Custom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Overlay Monitor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = { /* About dialog / info view */ }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About App",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Permission Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPermissionGranted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPermissionGranted) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = "Permission Status",
                            tint = if (isPermissionGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isPermissionGranted) "Permission Status" else "Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPermissionGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Text(
                        text = if (isPermissionGranted) {
                            "System Alert Window permission is active. The overlay can draw over other apps."
                        } else {
                            "To show the floating widget, Overlay Monitor requires overlay permissions. Grant this on the next system Settings page."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPermissionGranted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )

                    if (!isPermissionGranted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("request_permission_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SERVICE CONTROL",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartService,
                        enabled = isPermissionGranted && !isServiceRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("start_overlay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Start Overlay", fontWeight = FontWeight.Medium)
                        }
                    }

                    Button(
                        onClick = onStopService,
                        enabled = isServiceRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("stop_overlay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.White.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stop Overlay", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Metric selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "METRIC SELECTION",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Battery toggle card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔋 Battery Temp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Display real-time temperature in °C",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showBatteryTemp,
                            onCheckedChange = onToggleBattery,
                            modifier = Modifier.testTag("battery_temp_switch")
                        )
                    }
                }

                // RAM toggle card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💾 RAM Usage",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Percentage of active memory used",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showRamUsage,
                            onCheckedChange = onToggleRam,
                            modifier = Modifier.testTag("ram_usage_switch")
                        )
                    }
                }

                // CPU Temperature toggle card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌡️ CPU Temp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Display internal CPU temperature block",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showCpuTemp,
                            onCheckedChange = onToggleCpuTemp,
                            modifier = Modifier.testTag("cpu_temp_switch")
                        )
                    }
                }

                // CPU Usage toggle card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚙️ CPU Usage",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Display overall calculated CPU active load %",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showCpuUsage,
                            onCheckedChange = onToggleCpuUsage,
                            modifier = Modifier.testTag("cpu_usage_switch")
                        )
                    }
                }
            }

            // Preview simulation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ROG OVERLAY LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )

                // The pill-shaped preview bar: translucent dark backdrop, white Monospace text, separated by ' | '
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(Color(0x99000000), shape = RoundedCornerShape(19.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(19.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeItems = mutableListOf<String>()

                        val batteryTemp by TelemetryEngine.batteryTempFlow.collectAsState()
                        val ramUsage by TelemetryEngine.ramUsageFlow.collectAsState()
                        val cpuTemp by TelemetryEngine.cpuTempFlow.collectAsState()
                        val cpuUsage by TelemetryEngine.cpuUsageFlow.collectAsState()

                        if (showBatteryTemp) {
                            val tempStr = if (batteryTemp != null) "${String.format("%.1f", batteryTemp)}°C" else "--°C"
                            activeItems.add("🔋 $tempStr")
                        }
                        if (showRamUsage) {
                            val ramStr = if (ramUsage != null) "${ramUsage}%" else "--%"
                            activeItems.add("💾 RAM:$ramStr")
                        }
                        if (showCpuTemp) {
                            val t = cpuTemp
                            val cpuTempStr = if (t != null) {
                                if (t < 0f) "N/A" else "${String.format("%.1f", t)}°C"
                            } else "--°C"
                            activeItems.add("🌡️ $cpuTempStr")
                        }
                        if (showCpuUsage) {
                            val cpuUseStr = if (cpuUsage != null) "${cpuUsage}%" else "--%"
                            activeItems.add("⚙️ CPU:$cpuUseStr")
                        }

                        if (activeItems.isEmpty()) {
                            Text(
                                text = "Metrics Off",
                                color = Color.White.copy(alpha = 0.6f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                fontSize = 11.sp
                            )
                        } else {
                            val joinedText = activeItems.joinToString(" | ")
                            Text(
                                text = joinedText,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
