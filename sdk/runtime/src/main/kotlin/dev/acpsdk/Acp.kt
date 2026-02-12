package dev.acpsdk

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.acpsdk.annotations.AcpToolHandler
import java.util.concurrent.CopyOnWriteArrayList

/**
 * ACP SDK singleton entry point.
 * Manages tool handlers, screen tracking, and tool routing.
 */
object Acp {
    lateinit var context: Context
        private set

    internal var currentScreen: String? = null
        private set

    private val handlers = CopyOnWriteArrayList<AcpToolHandler>()

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun add(handler: AcpToolHandler) {
        handlers.add(handler)
    }

    fun remove(handler: AcpToolHandler) {
        handlers.remove(handler)
    }

    @Composable
    fun screen(name: String) {
        DisposableEffect(name) {
            currentScreen = name
            onDispose {
                if (currentScreen == name) currentScreen = null
            }
        }
    }

    internal fun routeTool(name: String, params: Map<String, Any?>): Map<String, Any?>? {
        for (handler in handlers) {
            val result = handler.onToolCall(name, params)
            if (result != null) return result
        }
        return null
    }

    internal fun getScreenName(): String? = currentScreen
}
