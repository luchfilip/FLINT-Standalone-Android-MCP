package dev.mcphub.acp.compiler

/**
 * Converts camelCase to snake_case.
 * Examples: "playlistId" → "playlist_id", "query" → "query", "trackIndex" → "track_index"
 */
fun camelToSnakeCase(name: String): String {
    return buildString {
        name.forEachIndexed { index, char ->
            if (char.isUpperCase()) {
                if (index > 0) append('_')
                append(char.lowercaseChar())
            } else {
                append(char)
            }
        }
    }
}

/**
 * Maps Kotlin type names to JSON Schema type strings.
 */
fun kotlinTypeToJsonSchema(typeName: String): Pair<String, Map<String, Any>?> {
    return when (typeName) {
        "kotlin.String", "String" -> "string" to null
        "kotlin.Int", "Int" -> "integer" to null
        "kotlin.Long", "Long" -> "integer" to null
        "kotlin.Double", "Double" -> "number" to null
        "kotlin.Float", "Float" -> "number" to null
        "kotlin.Boolean", "Boolean" -> "boolean" to null
        "kotlin.collections.List<kotlin.String>", "List<String>" -> "array" to mapOf("items" to mapOf("type" to "string"))
        else -> throw IllegalArgumentException("Unsupported type for ACP: $typeName")
    }
}

/**
 * Generates the Kotlin expression to extract a parameter from a Map<String, Any?>.
 */
fun kotlinTypeToExtractor(typeName: String, key: String, hasDefault: Boolean): String {
    val mapGet = "params[\"$key\"]"
    return when (typeName) {
        "kotlin.String", "String" ->
            if (hasDefault) "$mapGet as? String ?: ${key}" else "$mapGet as String"
        "kotlin.Int", "Int" ->
            if (hasDefault) "($mapGet as? Number)?.toInt() ?: $key" else "($mapGet as Number).toInt()"
        "kotlin.Long", "Long" ->
            if (hasDefault) "($mapGet as? Number)?.toLong() ?: $key" else "($mapGet as Number).toLong()"
        "kotlin.Double", "Double" ->
            if (hasDefault) "($mapGet as? Number)?.toDouble() ?: $key" else "($mapGet as Number).toDouble()"
        "kotlin.Float", "Float" ->
            if (hasDefault) "($mapGet as? Number)?.toFloat() ?: $key" else "($mapGet as Number).toFloat()"
        "kotlin.Boolean", "Boolean" ->
            if (hasDefault) "$mapGet as? Boolean ?: $key" else "$mapGet as Boolean"
        "kotlin.collections.List<kotlin.String>", "List<String>" ->
            "@Suppress(\"UNCHECKED_CAST\") ($mapGet as List<String>)"
        else -> throw IllegalArgumentException("Unsupported type for ACP: $typeName")
    }
}
