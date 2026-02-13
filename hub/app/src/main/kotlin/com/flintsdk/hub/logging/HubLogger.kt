package com.flintsdk.hub.logging

import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String)
    val recentLogs: StateFlow<List<LogEntry>>
    fun clear()
}

@Singleton
class HubLogger @Inject constructor() : Logger {

    private val lock = ReentrantReadWriteLock()
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)

    private val _recentLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val recentLogs: StateFlow<List<LogEntry>> = _recentLogs.asStateFlow()

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
        _recentLogs.value = lock.read { buffer.toList() }

        val logMessage = "[$tag] $message"
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(TAG, logMessage)
            LogLevel.INFO -> android.util.Log.i(TAG, logMessage)
            LogLevel.WARN -> android.util.Log.w(TAG, logMessage)
            LogLevel.ERROR -> android.util.Log.e(TAG, logMessage)
        }
    }

    override fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    override fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    override fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    override fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun getRecentLogs(): List<LogEntry> = lock.read {
        buffer.toList()
    }

    override fun clear() = lock.write {
        buffer.clear()
        _recentLogs.value = emptyList()
    }

    companion object {
        private const val MAX_ENTRIES = 100
        private const val TAG = "FlintHub"
    }
}
