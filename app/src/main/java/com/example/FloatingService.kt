package com.example

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class FloatingService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        var isRunning = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val viewStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = viewStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    private val showBatteryFlow = MutableStateFlow(true)
    private val showRamFlow = MutableStateFlow(true)
    private val showCpuTempFlow = MutableStateFlow(true)
    private val showCpuUsageFlow = MutableStateFlow(true)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        isRunning = true

        // Load SharedPreferences states
        val prefs = getSharedPreferences("overlay_monitor_prefs", Context.MODE_PRIVATE)
        showBatteryFlow.value = prefs.getBoolean("show_battery_temp", true)
        showRamFlow.value = prefs.getBoolean("show_ram_usage", true)
        showCpuTempFlow.value = prefs.getBoolean("show_cpu_temp", true)
        showCpuUsageFlow.value = prefs.getBoolean("show_cpu_usage", true)

        // Track Shared Preferences changes
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        TelemetryEngine.start(this)
        setupOverlayWindow()
    }

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        when (key) {
            "show_battery_temp" -> showBatteryFlow.value = sharedPrefs.getBoolean(key, true)
            "show_ram_usage" -> showRamFlow.value = sharedPrefs.getBoolean(key, true)
            "show_cpu_temp" -> showCpuTempFlow.value = sharedPrefs.getBoolean(key, true)
            "show_cpu_usage" -> showCpuUsageFlow.value = sharedPrefs.getBoolean(key, true)
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
            x = 100
            y = 150
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingService)
            setViewTreeViewModelStoreOwner(this@FloatingService)
            setViewTreeSavedStateRegistryOwner(this@FloatingService)

            setContent {
                val batteryTemp by TelemetryEngine.batteryTempFlow.collectAsState()
                val ramUsage by TelemetryEngine.ramUsageFlow.collectAsState()
                val cpuTemp by TelemetryEngine.cpuTempFlow.collectAsState()
                val cpuUsage by TelemetryEngine.cpuUsageFlow.collectAsState()

                val showBattery by showBatteryFlow.collectAsState()
                val showRam by showRamFlow.collectAsState()
                val showCpuTempState by showCpuTempFlow.collectAsState()
                val showCpuUsageState by showCpuUsageFlow.collectAsState()

                OverlayWidgetContent(
                    batteryTemp = batteryTemp,
                    ramUsage = ramUsage,
                    cpuTemp = cpuTemp,
                    cpuUsage = cpuUsage,
                    showBattery = showBattery,
                    showRam = showRam,
                    showCpuTemp = showCpuTempState,
                    showCpuUsage = showCpuUsageState
                )
            }
        }

        // Draggability Logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView?.setOnTouchListener { _, event ->
            val winManager = windowManager ?: return@setOnTouchListener false
            val view = composeView ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        winManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                else -> false
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    @Composable
    private fun OverlayWidgetContent(
        batteryTemp: Float?,
        ramUsage: Int?,
        cpuTemp: Float?,
        cpuUsage: Int?,
        showBattery: Boolean,
        showRam: Boolean,
        showCpuTemp: Boolean,
        showCpuUsage: Boolean
    ) {
        Row(
            modifier = Modifier
                .background(Color(0x99000000), shape = RoundedCornerShape(percent = 50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag grip indicator emoji
            Text(
                text = "✥ ",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            val displayLabel = buildString {
                val items = mutableListOf<String>()
                if (showBattery) {
                    val tempStr = if (batteryTemp != null) "${String.format("%.1f", batteryTemp)}°C" else "--°C"
                    items.add("🔋 $tempStr")
                }
                if (showRam) {
                    val ramStr = if (ramUsage != null) "$ramUsage%" else "--%"
                    items.add("💾 RAM:$ramStr")
                }
                if (showCpuTemp) {
                    val t = cpuTemp
                    val cpuTempStr = if (t != null) {
                        if (t < 0f) "N/A" else "${String.format("%.1f", t)}°C"
                    } else "--°C"
                    items.add("🌡️ $cpuTempStr")
                }
                if (showCpuUsage) {
                    val cpuUseStr = if (cpuUsage != null) "$cpuUsage%" else "--%"
                    items.add("⚙️ CPU:$cpuUseStr")
                }
                if (items.isEmpty()) {
                    append("Metrics Off")
                } else {
                    append(items.joinToString(" | "))
                }
            }

            Text(
                text = displayLabel,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }

    override fun onDestroy() {
        isRunning = false

        // Unregister Shared Preferences listeners to avoid leaks
        val prefs = getSharedPreferences("overlay_monitor_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewStore.clear()

        try {
            composeView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
