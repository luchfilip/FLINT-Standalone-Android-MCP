package dev.mcphub.acp.annotations

/**
 * Marks a class as an ACP tool host.
 * KSP will generate an AcpRouter for each annotated class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcpToolHost

/**
 * Marks a function as an ACP tool.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcpTool(
    val name: String,
    val description: String,
    val target: String
)

/**
 * Marks a function parameter with metadata for schema generation.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcpParam(
    val description: String,
    val name: String = ""
)

/**
 * Platform-agnostic tool handler interface.
 * Uses Map instead of Bundle so annotations module stays pure Kotlin.
 */
interface AcpToolHandler {
    fun onToolCall(name: String, params: Map<String, Any?>): Map<String, Any?>?
}
