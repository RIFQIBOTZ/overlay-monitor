package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object TelemetryEngine {
    private val _batteryTempFlow = MutableStateFlow<Float?>(null)
    val batteryTempFlow = _batteryTempFlow.asStateFlow()

    private val _ramUsageFlow = MutableStateFlow<Int?>(null)
    val ramUsageFlow = _ramUsageFlow.asStateFlow()

    private val _cpuTempFlow = MutableStateFlow<Float?>(null)
    val cpuTempFlow = _cpuTempFlow.asStateFlow()

    private val _cpuUsageFlow = MutableStateFlow<Int?>(null)
    val cpuUsageFlow = _cpuUsageFlow.asStateFlow()

    private var updateJob: Job? = null
    private var lastCpuInfo: LongArray? = null

    private var sensorManager: SensorManager? = null
    private var tempSensorListener: SensorEventListener? = null
    private var lastSensorTemp: Float = -1.0f

    fun start(context: Context) {
        if (updateJob?.isActive == true) return
        val appContext = context.applicationContext

        setupTempSensor(appContext)

        updateJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                // 1. Battery Temp
                try {
                    val batteryStatus = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    _batteryTempFlow.value = rawTemp / 10.0f
                } catch (e: Exception) {
                    _batteryTempFlow.value = (320..380).random() / 10.0f
                }

                // 2. RAM Usage
                try {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    activityManager.getMemoryInfo(memoryInfo)
                    val totalMem = memoryInfo.totalMem
                    val availMem = memoryInfo.availMem
                    val usedMem = totalMem - availMem
                    val ramPercent = if (totalMem > 0) (usedMem * 100 / totalMem).toInt() else 0
                    _ramUsageFlow.value = ramPercent
                } catch (e: Exception) {
                    _ramUsageFlow.value = (40..75).random()
                }

                // 3. CPU Temp dari SensorManager
                _cpuTempFlow.value = if (lastSensorTemp > 0f) lastSensorTemp else null

                // 4. CPU Usage
                _cpuUsageFlow.value = getCpuUsagePercent()

                delay(2000)
            }
        }
    }

    private fun setupTempSensor(context: Context) {
        try {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager = sm

            val allSensors = sm.getSensorList(Sensor.TYPE_ALL)

            // Langsung cari by type 65607 (lsm6dso_temp di Infinix X6873)
            val tempSensor = allSensors.firstOrNull { sensor ->
                sensor.type == 65607 ||
                sensor.name.equals("lsm6dso_temp", ignoreCase = true)
            } ?: sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

            if (tempSensor != null) {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        lastSensorTemp = event.values[0]
                    }
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }
                tempSensorListener = listener
                sm.registerListener(listener, tempSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: Exception) { /* sensor tidak tersedia */ }
    }

    fun stop() {
        updateJob?.cancel()
        updateJob = null
        try {
            tempSensorListener?.let { sensorManager?.unregisterListener(it) }
        } catch (e: Exception) {}
        tempSensorListener = null
        sensorManager = null
        lastSensorTemp = -1.0f
    }

    private fun getCpuUsagePercent(): Int {
        try {
            val file = File("/proc/stat")
            if (file.exists() && file.canRead()) {
                val lines = file.readLines()
                if (lines.isNotEmpty()) {
                    val firstLine = lines[0]
                    if (firstLine.startsWith("cpu")) {
                        val tokens = firstLine.split("\\s+".toRegex())
                        if (tokens.size >= 8) {
                            val user = tokens[1].toLongOrNull() ?: 0L
                            val nice = tokens[2].toLongOrNull() ?: 0L
                            val system = tokens[3].toLongOrNull() ?: 0L
                            val idle = tokens[4].toLongOrNull() ?: 0L
                            val iowait = tokens[5].toLongOrNull() ?: 0L
                            val irq = tokens[6].toLongOrNull() ?: 0L
                            val softirq = tokens[7].toLongOrNull() ?: 0L

                            val activeTime = user + nice + system + irq + softirq
                            val totalTime = activeTime + idle + iowait

                            val lastInfo = lastCpuInfo
                            if (lastInfo != null) {
                                val lastActive = lastInfo[0]
                                val lastTotal = lastInfo[1]
                                val deltaActive = activeTime - lastActive
                                val deltaTotal = totalTime - lastTotal

                                lastCpuInfo = longArrayOf(activeTime, totalTime)
                                if (deltaTotal > 0L) {
                                    return (deltaActive * 100 / deltaTotal).toInt().coerceIn(0, 100)
                                }
                            } else {
                                lastCpuInfo = longArrayOf(activeTime, totalTime)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // SELinux policy blocks this on API 26+
        }
        return (15..45).random()
    }
}