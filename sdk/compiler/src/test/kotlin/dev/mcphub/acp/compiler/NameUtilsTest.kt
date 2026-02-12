package dev.mcphub.acp.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NameUtilsTest {

    @Test
    fun `camelToSnakeCase converts simple names`() {
        assertEquals("query", camelToSnakeCase("query"))
        assertEquals("playlist_id", camelToSnakeCase("playlistId"))
        assertEquals("track_index", camelToSnakeCase("trackIndex"))
        assertEquals("my_long_name", camelToSnakeCase("myLongName"))
    }

    @Test
    fun `kotlinTypeToJsonSchema maps basic types`() {
        assertEquals("string" to null, kotlinTypeToJsonSchema("kotlin.String"))
        assertEquals("integer" to null, kotlinTypeToJsonSchema("kotlin.Int"))
        assertEquals("integer" to null, kotlinTypeToJsonSchema("kotlin.Long"))
        assertEquals("number" to null, kotlinTypeToJsonSchema("kotlin.Double"))
        assertEquals("number" to null, kotlinTypeToJsonSchema("kotlin.Float"))
        assertEquals("boolean" to null, kotlinTypeToJsonSchema("kotlin.Boolean"))
    }

    @Test
    fun `kotlinTypeToJsonSchema maps List of String`() {
        val (type, extra) = kotlinTypeToJsonSchema("kotlin.collections.List<kotlin.String>")
        assertEquals("array", type)
        assertNotNull(extra)
        @Suppress("UNCHECKED_CAST")
        val items = extra!!["items"] as Map<String, String>
        assertEquals("string", items["type"])
    }

    @Test
    fun `kotlinTypeToJsonSchema rejects unsupported types`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinTypeToJsonSchema("kotlin.collections.Map")
        }
    }

    @Test
    fun `kotlinTypeToExtractor generates correct extractors`() {
        assertEquals("params[\"query\"] as String", kotlinTypeToExtractor("kotlin.String", "query", false))
        assertEquals("params[\"query\"] as? String ?: query", kotlinTypeToExtractor("kotlin.String", "query", true))
        assertEquals("(params[\"count\"] as Number).toInt()", kotlinTypeToExtractor("kotlin.Int", "count", false))
        assertEquals("params[\"flag\"] as Boolean", kotlinTypeToExtractor("kotlin.Boolean", "flag", false))
    }
}
