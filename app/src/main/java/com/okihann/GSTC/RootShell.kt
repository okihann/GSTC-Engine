package com.okihann.GSTC

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootShell {
    fun runCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "
")
            os.writeBytes("exit
")
            os.flush()
            process.waitFor()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readText()
        } catch (e: Exception) {
            ""
        }
    }
}