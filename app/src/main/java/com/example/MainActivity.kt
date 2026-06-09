package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

private const val PREFS_NAME       = "overlay_monitor_prefs"
private const val KEY_BATTERY_TEMP = "show_battery_temp"
private const val KEY_RAM_USAGE    = "show_ram_usage"
private const val KEY_CPU_TEMP     = "show_cpu_temp"
private const val KEY_CPU_USAGE    = "show_cpu_usage"

// Warna Game Genie
private val GGBackground  = Color(0xFF0D0D0D)
private val GGSurface     = Color(0xFF1A1A1A)
private val GGSurface2    = Color(0xFF222222)
private val GGOrange      = Color(0xFFFF6B00)
private val GGOrangeLight = Color(0xFFFF8C35)
private val GGWhite       = Color(0xFFFFFFFF)
private val GGGray        = Color(0xFF888888)
private val GGGrayLight   = Color(0xFFAAAAAA)
private val GGBorder      = Color(0xFF333333)

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val isPermissionGranted = mutableStateOf(false)
    private val isServiceRunning    = mutableStateOf(false)
    private val showBatteryTemp     = mutableStateOf(true)
    private val showRamUsage        = mutableStateOf(true)
    private val showCpuTemp         = mutableStateOf(true)
    private val showCpuUsage        = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        showBatteryTemp.value = prefs.getBoolean(KEY_BATTERY_TEMP, true)
        showRamUsage.value    = prefs.getBoolean(KEY_RAM_USAGE, true)
        showCpuTemp.value     = prefs.getBoolean(KEY_CPU_TEMP, true)
        showCpuUsage.value    = prefs.getBoolean(KEY_CPU_USAGE, true)

        TelemetryEngine.start(this)

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                GameGenieScreen(
                    isPermissionGranted = isPermissionGranted.value,
                    isServiceRunning    = isServiceRunning.value,
                    showBatteryTemp     = showBatteryTemp.value,
                    showRamUsage        = showRamUsage.value,
                    showCpuTemp         = showCpuTemp.value,
                    showCpuUsage        = showCpuUsage.value,
                    onToggleBattery  = { v -> showBatteryTemp.value = v; prefs.edit().putBoolean(KEY_BATTERY_TEMP, v).apply() },
                    onToggleRam      = { v -> showRamUsage.value    = v; prefs.edit().putBoolean(KEY_RAM_USAGE, v).apply() },
                    onToggleCpuTemp  = { v -> showCpuTemp.value     = v; prefs.edit().putBoolean(KEY_CPU_TEMP, v).apply() },
                    onToggleCpuUsage = { v -> showCpuUsage.value    = v; prefs.edit().putBoolean(KEY_CPU_USAGE, v).apply() },
                    onRequestPermission = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
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

    override fun onResume() {
        super.onResume()
        isPermissionGranted.value = Settings.canDrawOverlays(this)
        isServiceRunning.value    = FloatingService.isRunning
    }
}

@Composable
fun GameGenieScreen(
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
        modifier = Modifier
            .fillMaxSize()
            .background(GGBackground)
    ) {
        // Header bar - Game Genie style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .background(GGSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ROG-style icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Brush.linearGradient(listOf(GGOrange, GGOrangeLight)),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 16.sp)
                    }
                    Text(
                        text = "ROG Overlay",
                        color = GGWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isServiceRunning) Color(0xFF00FF88) else GGGray,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Permission card
            if (!isPermissionGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A1500))
                        .border(1.dp, GGOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⚠  Izin Diperlukan", color = GGOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Overlay Monitor butuh izin System Alert Window untuk tampil di atas aplikasi lain.",
                            color = GGGrayLight, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GGOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Beri Izin", color = GGWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Service control
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "KONTROL SERVICE",
                    color = GGOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartService,
                        enabled = isPermissionGranted && !isServiceRunning,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GGOrange,
                            disabledContainerColor = GGSurface2
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "▶  Start",
                            color = if (isPermissionGranted && !isServiceRunning) GGWhite else GGGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = onStopService,
                        enabled = isServiceRunning,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GGSurface2,
                            disabledContainerColor = GGSurface2
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "■  Stop",
                            color = if (isServiceRunning) GGWhite else GGGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Metric toggles
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "METRIK",
                    color = GGOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                GGMetricToggle(icon = "🔋", label = "Battery Temp",  desc = "Suhu baterai real-time",       checked = showBatteryTemp, onToggle = onToggleBattery)
                GGMetricToggle(icon = "💾", label = "RAM Usage",     desc = "Persentase RAM aktif",          checked = showRamUsage,    onToggle = onToggleRam)
                GGMetricToggle(icon = "🌡", label = "CPU Temp",      desc = "Suhu internal (lsm6dso)",      checked = showCpuTemp,     onToggle = onToggleCpuTemp)
                GGMetricToggle(icon = "⚙", label = "CPU Usage",     desc = "Beban CPU keseluruhan",         checked = showCpuUsage,    onToggle = onToggleCpuUsage)
            }

            // Live preview
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "PREVIEW OVERLAY",
                    color = GGGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                val batteryTemp by TelemetryEngine.batteryTempFlow.collectAsState()
                val ramUsage    by TelemetryEngine.ramUsageFlow.collectAsState()
                val cpuTemp     by TelemetryEngine.cpuTempFlow.collectAsState()
                val cpuUsage    by TelemetryEngine.cpuUsageFlow.collectAsState()

                val metrics = mutableListOf<String>()
                if (showBatteryTemp) metrics.add("▣ ${cpuUsage ?: "--"}%")
                if (showRamUsage)    metrics.add("▤ RAM:${ramUsage ?: "--"}%")
                val b = batteryTemp
                if (showBatteryTemp) metrics.add("▪ BAT:${if (b != null) String.format("%.0f", b) else "--"}%")
                val c = cpuTemp
                if (showCpuTemp)     metrics.add("▲ ${if (c != null && c > 0f) String.format("%.0f", c) else "--"}°C")

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xEE000000))
                        .border(1.dp, GGBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(0xFFCC0000), RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = GGWhite, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (metrics.isEmpty()) "Semua metrik off" else metrics.joinToString(" | "),
                            color = GGWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GGMetricToggle(
    icon: String,
    label: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GGSurface)
            .border(
                1.dp,
                if (checked) GGOrange.copy(alpha = 0.4f) else GGBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(icon, fontSize = 20.sp)
                Column {
                    Text(label, color = GGWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(desc, color = GGGray, fontSize = 11.sp)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GGWhite,
                    checkedTrackColor = GGOrange,
                    uncheckedThumbColor = GGGray,
                    uncheckedTrackColor = GGSurface2
                )
            )
        }
    }
}
