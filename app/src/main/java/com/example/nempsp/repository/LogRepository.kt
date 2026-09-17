package com.example.nempsp.repository

import com.example.nempsp.model.ConnectionMode
import com.example.nempsp.model.LogEntry
import com.example.nempsp.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

object LogRepository {
    private const val MAX_LOGS = 300
    private val logDeque = ConcurrentLinkedDeque<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        mode: ConnectionMode? = null
    ) {
        val entry = LogEntry(
            mode = mode,
            level = level,
            tag = tag,
            message = message
        )
        logDeque.addFirst(entry)
        while (logDeque.size > MAX_LOGS) {
            logDeque.pollLast()
        }
        _logsFlow.value = logDeque.toList()
    }

    fun info(tag: String, message: String, mode: ConnectionMode? = null) =
        log(LogLevel.INFO, tag, message, mode)

    fun success(tag: String, message: String, mode: ConnectionMode? = null) =
        log(LogLevel.SUCCESS, tag, message, mode)

    fun warn(tag: String, message: String, mode: ConnectionMode? = null) =
        log(LogLevel.WARN, tag, message, mode)

    fun error(tag: String, message: String, mode: ConnectionMode? = null) =
        log(LogLevel.ERROR, tag, message, mode)

    fun packet(tag: String, message: String, mode: ConnectionMode? = null) =
        log(LogLevel.PACKET, tag, message, mode)

    fun clear() {
        logDeque.clear()
        _logsFlow.value = emptyList()
    }
}
