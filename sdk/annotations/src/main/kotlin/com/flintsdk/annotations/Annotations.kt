package com.flintsdk.annotations

/**
 * Marks a class as an Flint tool host.
 * KSP will generate an FlintRouter for each annotated class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlintToolHost

/**
 * Marks a function as an Flint tool.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlintTool(
    val name: String,
    val description: String,
    val target: String = ""
)

/**
 * Marks a function parameter with metadata for schema generation.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlintParam(
    val description: String,
    val name: String = ""
)

data class FlintToolDescriptor(
    val name: String,
    val description: String,
    val params: List<FlintParamDescriptor> = emptyList()
)

data class FlintParamDescriptor(
    val name: String,
    val type: String,       // "string", "integer", "number", "boolean"
    val description: String,
    val required: Boolean = true
)

/**
 * Platform-agnostic tool handler interface.
 * Uses Map instead of Bundle so annotations module stays pure Kotlin.
 */
interface FlintToolHandler {
    fun onToolCall(name: String, params: Map<String, Any?>): Map<String, Any?>?
    fun describeTools(): List<FlintToolDescriptor> = emptyList()
}
