package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow

class FloatingService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        var isRunning = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val viewStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = viewStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    private val showBatteryFlow  = MutableStateFlow(true)
    private val showRamFlow      = MutableStateFlow(true)
    private val showCpuTempFlow  = MutableStateFlow(true)
    private val showCpuUsageFlow = MutableStateFlow(true)
    private val opacityFlow      = MutableStateFlow(90f)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        isRunning = true

        val prefs = getSharedPreferences("overlay_monitor_prefs", Context.MODE_PRIVATE)
        showBatteryFlow.value  = prefs.getBoolean("show_battery_temp", true)
        showRamFlow.value      = prefs.getBoolean("show_ram_usage", true)
        showCpuTempFlow.value  = prefs.getBoolean("show_cpu_temp", true)
        showCpuUsageFlow.value = prefs.getBoolean("show_cpu_usage", true)
        opacityFlow.value      = prefs.getFloat("overlay_opacity", 90f)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        TelemetryEngine.start(this)
        setupOverlayWindow()
    }

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        when (key) {
            "show_battery_temp" -> showBatteryFlow.value  = sharedPrefs.getBoolean(key, true)
            "show_ram_usage"    -> showRamFlow.value      = sharedPrefs.getBoolean(key, true)
            "show_cpu_temp"     -> showCpuTempFlow.value  = sharedPrefs.getBoolean(key, true)
            "show_cpu_usage"    -> showCpuUsageFlow.value = sharedPrefs.getBoolean(key, true)
            "overlay_opacity"   -> opacityFlow.value      = sharedPrefs.getFloat(key, 90f)
        }
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 80
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingService)
            setViewTreeViewModelStoreOwner(this@FloatingService)
            setViewTreeSavedStateRegistryOwner(this@FloatingService)

            setContent {
                val batteryTemp by TelemetryEngine.batteryTempFlow.collectAsState()
                val ramUsage    by TelemetryEngine.ramUsageFlow.collectAsState()
                val cpuTemp     by TelemetryEngine.cpuTempFlow.collectAsState()
                val cpuUsage    by TelemetryEngine.cpuUsageFlow.collectAsState()
                val showBattery   by showBatteryFlow.collectAsState()
                val showRam       by showRamFlow.collectAsState()
                val showCpuTempS  by showCpuTempFlow.collectAsState()
                val showCpuUsageS by showCpuUsageFlow.collectAsState()
                val opacity       by opacityFlow.collectAsState()

                RogOverlay(
                    batteryTemp  = batteryTemp,
                    ramUsage     = ramUsage,
                    cpuTemp      = cpuTemp,
                    cpuUsage     = cpuUsage,
                    showBattery  = showBattery,
                    showRam      = showRam,
                    showCpuTemp  = showCpuTempS,
                    showCpuUsage = showCpuUsageS,
                    bgOpacity    = opacity / 100f
                )
            }
        }

        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        composeView?.setOnTouchListener { _, event ->
            val wm   = windowManager ?: return@setOnTouchListener false
            val view = composeView   ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try { wm.updateViewLayout(view, params) } catch (e: Exception) {}
                    true
                }
                else -> false
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        try { windowManager?.addView(composeView, params) } catch (e: Exception) { stopSelf() }
    }

    @Composable
    private fun RogOverlay(
        batteryTemp: Float?, ramUsage: Int?, cpuTemp: Float?, cpuUsage: Int?,
        showBattery: Boolean, showRam: Boolean, showCpuTemp: Boolean, showCpuUsage: Boolean,
        bgOpacity: Float  // hanya background yang transparan, teks tetap solid
    ) {
        val rogRed    = Color(0xFFCC0000)
        val rogWhite  = Color(0xFFFFFFFF)
        val rogGray   = Color(0xFF888888)
        val rogYellow = Color(0xFFFFCC00)
        val rogCyan   = Color(0xFF00CCFF)
        val rogOrange = Color(0xFFFF6B00)

        // Hanya alpha background yang dipengaruhi opacity, warna teks tetap 1.0f
        val bgColor = Color(0xFF000000).copy(alpha = bgOpacity * 0.92f)

        data class Metric(val label: String, val value: String, val valueColor: Color)

        val metrics = mutableListOf<Metric>()
        if (showCpuUsage) metrics.add(Metric("CPU",  "${cpuUsage ?: "--"}%", rogYellow))
        if (showRam)      metrics.add(Metric("RAM",  "${ramUsage ?: "--"}%", rogCyan))
        if (showBattery)  metrics.add(Metric("BAT",  "${if (batteryTemp != null) String.format("%.1f", batteryTemp) else "--"}°C", rogWhite))
        if (showCpuTemp)  metrics.add(Metric("TEMP", "${if (cpuTemp != null && cpuTemp > 0f) String.format("%.1f", cpuTemp) else "--"}°C", rogOrange))

        if (metrics.isEmpty()) return

        Row(
            modifier = Modifier
                .background(bgColor, shape = RoundedCornerShape(3.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon ROG — tetap solid
            Box(
                modifier = Modifier.size(14.dp).background(rogRed, shape = RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = rogWhite, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.width(7.dp))

            metrics.forEachIndexed { index, metric ->
                if (index > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(rogGray.copy(alpha = 0.35f)))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Label dan nilai tetap solid (alpha 1.0f)
                    Text(
                        text = metric.label,
                        color = rogGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 9.sp
                    )
                    Text(
                        text = metric.value,
                        color = metric.valueColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        val prefs = getSharedPreferences("overlay_monitor_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewStore.clear()
        try { composeView?.let { windowManager?.removeView(it) } } catch (e: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
