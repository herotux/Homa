package com.goodwy.smsmessenger.helpers

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object HomaDiagnostics {
    private const val TAG = "HomaDiagnostics"
    private const val MAX_LOG_BYTES = 1024 * 1024
    private val executor = Executors.newSingleThreadExecutor()
    private val lock = Any()
    @Volatile private var initialized = false
    private lateinit var logFile: File

    fun init(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            logFile = File(context.applicationContext.filesDir, "homa_diagnostics.log")
            initialized = true
            log("APP_START", "pid=" + android.os.Process.myPid() + " version=" + context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        }
    }

    fun log(event: String, message: String = "") {
        val line = timestamp() + " [" + event + "] " + message
        Log.i(TAG, line)
        if (!initialized) return
        executor.execute {
            synchronized(lock) {
                try {
                    if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
                        val old = logFile.readText()
                        logFile.writeText(old.takeLast(MAX_LOG_BYTES / 2))
                    }
                    logFile.appendText(line + "\n")
                } catch (e: Exception) {
                    Log.e(TAG, "file logging failed", e)
                }
            }
        }
    }

    fun error(event: String, error: Throwable) {
        Log.e(TAG, event + ": " + error.message, error)
        log(event, error.javaClass.simpleName + ": " + error.message)
    }

    fun <T> timed(event: String, block: () -> T): T {
        val started = System.nanoTime()
        return try {
            block()
        } catch (e: Exception) {
            error(event, e)
            throw e
        } finally {
            val ms = (System.nanoTime() - started) / 1_000_000
            log(event, "durationMs=" + ms)
            if (ms >= 500) Log.w(TAG, "SLOW " + event + " " + ms + "ms")
        }
    }

    fun getLogFile(context: Context): File {
        if (!initialized) init(context)
        return logFile
    }

    private fun timestamp() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
