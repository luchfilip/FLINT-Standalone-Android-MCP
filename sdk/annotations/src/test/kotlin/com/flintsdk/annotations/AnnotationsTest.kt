package com.flintsdk.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

@FlintToolHost
class SampleToolHost {
    @FlintTool(name = "test_tool", description = "A test tool", target = "test_screen")
    fun testTool(
        @FlintParam(description = "A test param") query: String,
        @FlintParam(description = "Custom name", name = "custom_id") itemId: String
    ) {}
}

class AnnotationsTest {

    @Test
    fun `FlintToolHost is available at runtime`() {
        val annotation = SampleToolHost::class.findAnnotation<FlintToolHost>()
        assertNotNull(annotation)
    }

    @Test
    fun `FlintTool is available at runtime with correct values`() {
        val function = SampleToolHost::class.functions.first { it.name == "testTool" }
        val annotation = function.findAnnotation<FlintTool>()
        assertNotNull(annotation)
        assertEquals("test_tool", annotation!!.name)
        assertEquals("A test tool", annotation.description)
        assertEquals("test_screen", annotation.target)
    }

    @Test
    fun `FlintParam is available at runtime with correct values`() {
        val function = SampleToolHost::class.functions.first { it.name == "testTool" }
        val params = function.valueParameters

        val queryParam = params[0].findAnnotation<FlintParam>()
        assertNotNull(queryParam)
        assertEquals("A test param", queryParam!!.description)
        assertEquals("", queryParam.name)

        val itemParam = params[1].findAnnotation<FlintParam>()
        assertNotNull(itemParam)
        assertEquals("Custom name", itemParam!!.description)
        assertEquals("custom_id", itemParam.name)
    }

    @Test
    fun `FlintParam name defaults to empty string`() {
        val function = SampleToolHost::class.functions.first { it.name == "testTool" }
        val queryParam = function.valueParameters[0].findAnnotation<FlintParam>()!!
        assertEquals("", queryParam.name)
    }

    @Test
    fun `FlintToolHandler interface is implementable`() {
        val handler = object : FlintToolHandler {
            override fun onToolCall(name: String, params: Map<String, Any?>): Map<String, Any?>? {
                return if (name == "test") mapOf("result" to "ok") else null
            }
        }

        assertEquals(mapOf("result" to "ok"), handler.onToolCall("test", emptyMap()))
        assertNull(handler.onToolCall("unknown", emptyMap()))
    }
}
