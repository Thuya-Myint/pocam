package com.pocam.camera.performance

import android.os.Debug
import android.os.Process
import android.os.SystemClock
import java.io.File

data class AppPerformance(val cpuPercent: Int?, val memoryMb: Long)

class AppPerformanceMonitor {
    private var lastCpuMs = Process.getElapsedCpuTime()
    private var lastElapsedMs = SystemClock.elapsedRealtime()

    fun sample(): AppPerformance {
        val cpu = Process.getElapsedCpuTime()
        val elapsed = SystemClock.elapsedRealtime()
        val deltaElapsed = elapsed - lastElapsedMs
        val percent = if (deltaElapsed > 0) {
            (((cpu - lastCpuMs).toDouble() / deltaElapsed) * 100).toInt().coerceAtLeast(0)
        } else null
        lastCpuMs = cpu
        lastElapsedMs = elapsed
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        return AppPerformance(percent, info.totalPss.toLong() / 1024)
    }
}
