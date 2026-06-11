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

private const val PREFS_NAME        = "overlay_monitor_prefs"
private const val KEY_BATTERY_TEMP  = "show_battery_temp"
private const val KEY_RAM_USAGE     = "show_ram_usage"
private const val KEY_CPU_TEMP      = "show_cpu_temp"
private const val KEY_CPU_USAGE     = "show_cpu_usage"
private const val KEY_OPACITY       = "overlay_opacity"

private val BGColor     = Color(0xFF0A0A0A)
private val CardColor   = Color(0xFF141414)
private val CardColor2  = Color(0xFF1C1C1C)
private val Red         = Color(0xFFCC0000)
private val RedLight    = Color(0xFFFF2222)
private val White       = Color(0xFFFFFFFF)
private val Gray        = Color(0xFF666666)
private val GrayLight   = Color(0xFF999999)
private val Border      = Color(0xFF2A2A2A)
private val Yellow      = Color(0xFFFFCC00)
private val Cyan        = Color(0xFF00CCFF)
private val Green       = Color(0xFF00FF88)
private val OrangeWarm  = Color(0xFFFF6B00)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val isPermissionGranted = mutableStateOf(false)
    private val isServiceRunning    = mutableStateOf(false)
    private val showBatteryTemp     = mutableStateOf(true)
    private val showRamUsage        = mutableStateOf(true)
    private val showCpuTemp         = mutableStateOf(true)
    private val showCpuUsage        = mutableStateOf(true)
    private val overlayOpacity      = mutableStateOf(90f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showBatteryTemp.value = prefs.getBoolean(KEY_BATTERY_TEMP, true)
        showRamUsage.value    = prefs.getBoolean(KEY_RAM_USAGE, true)
        showCpuTemp.value     = prefs.getBoolean(KEY_CPU_TEMP, true)
        showCpuUsage.value    = prefs.getBoolean(KEY_CPU_USAGE, true)
        overlayOpacity.value  = prefs.getFloat(KEY_OPACITY, 90f)
        TelemetryEngine.start(this)
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                MainScreen(
                    isPermissionGranted = isPermissionGranted.value,
                    isServiceRunning    = isServiceRunning.value,
                    showBatteryTemp     = showBatteryTemp.value,
                    showRamUsage        = showRamUsage.value,
                    showCpuTemp         = showCpuTemp.value,
                    showCpuUsage        = showCpuUsage.value,
                    overlayOpacity      = overlayOpacity.value,
                    onToggleBattery  = { v -> showBatteryTemp.value = v; prefs.edit().putBoolean(KEY_BATTERY_TEMP, v).apply() },
                    onToggleRam      = { v -> showRamUsage.value    = v; prefs.edit().putBoolean(KEY_RAM_USAGE, v).apply() },
                    onToggleCpuTemp  = { v -> showCpuTemp.value     = v; prefs.edit().putBoolean(KEY_CPU_TEMP, v).apply() },
                    onToggleCpuUsage = { v -> showCpuUsage.value    = v; prefs.edit().putBoolean(KEY_CPU_USAGE, v).apply() },
                    onOpacityChange  = { v -> overlayOpacity.value  = v; prefs.edit().putFloat(KEY_OPACITY, v).apply() },
                    onRequestPermission = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                    onStartService = { startService(Intent(this, FloatingService::class.java)); isServiceRunning.value = true },
                    onStopService  = { stopService(Intent(this, FloatingService::class.java)); isServiceRunning.value = false }
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
fun MainScreen(
    isPermissionGranted: Boolean,
    isServiceRunning: Boolean,
    showBatteryTemp: Boolean,
    showRamUsage: Boolean,
    showCpuTemp: Boolean,
    showCpuUsage: Boolean,
    overlayOpacity: Float,
    onToggleBattery: (Boolean) -> Unit,
    onToggleRam: (Boolean) -> Unit,
    onToggleCpuTemp: (Boolean) -> Unit,
    onToggleCpuUsage: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val batteryTemp by TelemetryEngine.batteryTempFlow.collectAsState()
    val ramUsage    by TelemetryEngine.ramUsageFlow.collectAsState()
    val cpuTemp     by TelemetryEngine.cpuTempFlow.collectAsState()
    val cpuUsage    by TelemetryEngine.cpuUsageFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(BGColor)
    ) {
        // ── Header compact ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), BGColor)))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Brush.radialGradient(listOf(RedLight, Red)), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("⚡", fontSize = 17.sp) }
                Text("ROG Overlay", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .background(
                        if (isServiceRunning) Green.copy(alpha = 0.15f) else Gray.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(5.dp).background(if (isServiceRunning) Green else Gray, RoundedCornerShape(50)))
                    Text(if (isServiceRunning) "ON" else "OFF", color = if (isServiceRunning) Green else Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Permission (conditional) ─────────────────────────
            if (!isPermissionGranted) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A0000)).border(1.dp, Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⚠", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Izin Diperlukan", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Butuh izin System Alert Window", color = GrayLight, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Red),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) { Text("Izinkan", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // ── STATUS LIVE 2x2 ─────────────────────────────────
            val bTemp = batteryTemp
            val cTemp = cpuTemp
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactStatCard(modifier = Modifier.weight(1f), icon = "⚙", label = "CPU", value = "${cpuUsage ?: "--"}%", valueColor = Yellow)
                CompactStatCard(modifier = Modifier.weight(1f), icon = "💾", label = "RAM", value = "${ramUsage ?: "--"}%", valueColor = Cyan)
                CompactStatCard(modifier = Modifier.weight(1f), icon = "🔋", label = "BAT", value = "${if (bTemp != null) String.format("%.1f", bTemp) else "--"}°C", valueColor = Green)
                CompactStatCard(modifier = Modifier.weight(1f), icon = "🌡", label = "TEMP", value = "${if (cTemp != null && cTemp > 0f) String.format("%.1f", cTemp) else "--"}°C", valueColor = OrangeWarm)
            }

            // ── Start/Stop + Opacity dalam satu card ─────────────
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardColor).padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Start/Stop
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (isPermissionGranted && !isServiceRunning) Brush.linearGradient(listOf(RedLight, Red)) else Brush.linearGradient(listOf(CardColor2, CardColor2))),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onStartService,
                                enabled = isPermissionGranted && !isServiceRunning,
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                                shape = RoundedCornerShape(10.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                            ) { Text("▶  Start", color = if (isPermissionGranted && !isServiceRunning) White else Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(CardColor2),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onStopService,
                                enabled = isServiceRunning,
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                                shape = RoundedCornerShape(10.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                            ) { Text("■  Stop", color = if (isServiceRunning) White else Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                    }
                    // Opacity
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("BG", color = Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = overlayOpacity, onValueChange = onOpacityChange, valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red, inactiveTrackColor = Border)
                        )
                        Text("${overlayOpacity.toInt()}%", color = Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // ── Metrik toggles compact dalam satu card ───────────
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardColor)) {
                Column {
                    CompactToggleRow("⚙", "CPU Usage",    showCpuUsage,    onToggleCpuUsage,  true)
                    CompactToggleRow("💾", "RAM Usage",    showRamUsage,    onToggleRam,       true)
                    CompactToggleRow("🔋", "Battery Temp", showBatteryTemp, onToggleBattery,   true)
                    CompactToggleRow("🌡", "CPU Temp",     showCpuTemp,     onToggleCpuTemp,   false)
                }
            }

            // ── Preview ──────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardColor).padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PREVIEW", color = Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

                    val bgAlpha = overlayOpacity / 100f
                    val metrics = mutableListOf<Triple<String, String, Color>>()
                    if (showCpuUsage)    metrics.add(Triple("CPU",  "${cpuUsage ?: "--"}%", Yellow))
                    if (showRamUsage)    metrics.add(Triple("RAM",  "${ramUsage ?: "--"}%", Cyan))
                    val bT = batteryTemp; val cT = cpuTemp
                    if (showBatteryTemp) metrics.add(Triple("BAT",  "${if (bT != null) String.format("%.1f", bT) else "--"}°C", Green))
                    if (showCpuTemp)     metrics.add(Triple("TEMP", "${if (cT != null && cT > 0f) String.format("%.1f", cT) else "--"}°C", OrangeWarm))

                    if (metrics.isNotEmpty()) {
                        Row(
                            modifier = Modifier.background(Color(0xFF000000).copy(alpha = bgAlpha * 0.92f), RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(14.dp).background(Red, RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                                Text("✕", color = White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            metrics.forEachIndexed { i, (label, value, color) ->
                                if (i > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0xFF888888).copy(alpha = 0.35f)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, color = Color(0xFF888888), fontSize = 8.sp, fontFamily = FontFamily.Monospace, lineHeight = 9.sp)
                                    Text(value, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                                }
                            }
                        }
                    } else {
                        Text("Semua metrik dinonaktifkan", color = Gray, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun CompactStatCard(modifier: Modifier = Modifier, icon: String, label: String, value: String, valueColor: Color) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(CardColor).border(1.dp, Border, RoundedCornerShape(12.dp)).padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 14.sp)
            Text(label, color = Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun CompactToggleRow(icon: String, label: String, checked: Boolean, onToggle: (Boolean) -> Unit, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).background(if (checked) Red.copy(alpha = 0.15f) else CardColor2, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(icon, fontSize = 14.sp) }
                Text(label, color = White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
            Switch(
                checked = checked, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = Red, uncheckedThumbColor = Gray, uncheckedTrackColor = CardColor2)
            )
        }
        if (showDivider) Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 14.dp).background(Border))
    }
}