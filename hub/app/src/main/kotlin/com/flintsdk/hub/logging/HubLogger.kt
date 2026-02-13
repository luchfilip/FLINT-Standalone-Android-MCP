package com.flintsdk.hub.logging

import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.read
import kotlin.concurrent.write

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String
)

@Singleton
class HubLogger @Inject constructor() {

    private val lock = ReentrantReadWriteLock()
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)

    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = Instant.now(),
            level = level,
            tag = tag,
            message = message
        )
        lock.write {
            if (buffer.size >= MAX_ENTRIES) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
        }
        // Also mirror to Android logcat
        val logMessage = "[$tag] $message"
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(TAG, logMessage)
            LogLevel.INFO -> android.util.Log.i(TAG, logMessage)
            LogLevel.WARN -> android.util.Log.w(TAG, logMessage)
            LogLevel.ERROR -> android.util.Log.e(TAG, logMessage)
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun getRecentLogs(): List<LogEntry> = lock.read {
        buffer.toList()
    }

    fun clear() = lock.write {
        buffer.clear()
    }

    companion object {
        private const val MAX_ENTRIES = 100
        private const val TAG = "FlintHub"
    }
}
