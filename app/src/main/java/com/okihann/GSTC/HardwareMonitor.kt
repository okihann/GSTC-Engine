package com.okihann.GSTC

object HardwareMonitor {
    
    private var lastTotalCpu = 0L
    private var lastIdleCpu = 0L

    fun getForegroundApp(): String {
        return try {
            val raw = RootShell.runCommand("dumpsys window | grep -E 'mCurrentFocus'").trim()
            if (raw.contains("Window{") && raw.contains("/")) {
                raw.substringAfter("Window{").substringBefore("/").split(" ").last()
            } else "unknown_system_ui"
        } catch (_: Exception) { "unknown_system_ui" }
    }

    data class DeviceStats(
        val cpuUsage: Int,
        val cpuTemp: Int,
        val gpuUsage: Int,
        val gpuTemp: Int,
        val gpuFreq: Int,
        val battTemp: Int
    )

    fun fetchAllStats(): DeviceStats {
        val script = """
            read -r cpu user nice system idle iowait irq softirq steal guest guest_nice < /proc/stat
            CPU_STR="${'$'}user ${'$'}nice ${'$'}system ${'$'}idle ${'$'}iowait ${'$'}irq ${'$'}softirq"
            
            CPU_T=${'$'}(for z in /sys/class/thermal/thermal_zone*; do if grep -qiE "cpu|soc|tsens" "${'$'}z/type" 2>/dev/null; then cat "${'$'}z/temp" 2>/dev/null; fi; done | sort -nr | head -n 1)
            GPU_T=${'$'}(cat /sys/class/kgsl/kgsl-3d0/temp 2>/dev/null)
            GPU_U=${'$'}(cat /sys/class/kgsl/kgsl-3d0/gpubusy 2>/dev/null)
            
            GPU_F=${'$'}(cat /sys/class/kgsl/kgsl-3d0/gpuclk 2>/dev/null || cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq 2>/dev/null | head -n 1)
            
            BATT_T=${'$'}(for z in /sys/class/thermal/thermal_zone*; do if grep -qix "battery" "${'$'}z/type" 2>/dev/null; then cat "${'$'}z/temp" 2>/dev/null; break; fi; done)
            
            echo "${'$'}CPU_STR,${'$'}CPU_T,${'$'}GPU_T,${'$'}GPU_U,${'$'}GPU_F,${'$'}BATT_T" | tr -d '
'
        """.trimIndent()

        val rawOutput = try {
            RootShell.runCommand(script).trim()
        } catch (_: Exception) { "" }

        var cpuUsage = 0
        var cpuTemp = 0
        var gpuTemp = 0
        var gpuUsage = 0
        var gpuFreq = 0
        var battTemp = 0

        try {
            val parts = rawOutput.split(",")
            if (parts.size >= 6) {
                
                val cpuParts = parts[0].trim().split(Regex("\s+")).mapNotNull { it.toLongOrNull() }
                if (cpuParts.size >= 7) {
                    val user = cpuParts[0]
                    val nice = cpuParts[1]
                    val system = cpuParts[2]
                    val idle = cpuParts[3]
                    val iowait = cpuParts[4]
                    val irq = cpuParts[5]
                    val softirq = cpuParts[6]

                    val totalIdle = idle + iowait
                    val totalNonIdle = user + nice + system + irq + softirq
                    val total = totalIdle + totalNonIdle

                    val diffTotal = total - lastTotalCpu
                    val diffIdle = totalIdle - lastIdleCpu

                    lastTotalCpu = total
                    lastIdleCpu = totalIdle

                    if (diffTotal > 0) cpuUsage = ((diffTotal - diffIdle) * 100 / diffTotal).toInt()
                }

                val rawCpuTemp = parts[1].trim().toIntOrNull() ?: 0
                cpuTemp = if (rawCpuTemp > 1000) rawCpuTemp / 1000 else rawCpuTemp

                val rawGpuTemp = parts[2].trim().toIntOrNull() ?: 0
                gpuTemp = if (rawGpuTemp > 1000) rawGpuTemp / 1000 else rawGpuTemp

                val gpuBusyParts = parts[3].trim().split(Regex("\s+"))
                if (gpuBusyParts.size >= 2) {
                    val busy = gpuBusyParts[0].toFloatOrNull() ?: 0f
                    val total = gpuBusyParts[1].toFloatOrNull() ?: 0f
                    if (total > 0) gpuUsage = ((busy / total) * 100).toInt()
                }

                val rawGpuFreq = parts[4].trim().toLongOrNull() ?: 0L
                gpuFreq = if (rawGpuFreq > 10000) (rawGpuFreq / 1000000).toInt() else rawGpuFreq.toInt()

                val rawBattTemp = parts[5].trim().toIntOrNull() ?: 0
                battTemp = if (rawBattTemp > 1000) rawBattTemp / 1000 else if (rawBattTemp > 100) rawBattTemp / 10 else rawBattTemp
            }
        } catch (_: Exception) {}

        return DeviceStats(cpuUsage, cpuTemp, gpuUsage, gpuTemp, gpuFreq, battTemp)
    }
}