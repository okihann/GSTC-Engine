package com.okihann.GSTC

object SessionLogger {
    fun clearSession() {}
    fun logData(fps: Int, cpuU: Int, cpuT: Int, gpuU: Int, gpuF: Int, gpuT: Int, battT: Int) {}
    fun finishSession(games: List<String>) {}
}