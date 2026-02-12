package dev.mcphub.acp.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

@AcpToolHost
class SampleToolHost {
    @AcpTool(name = "test_tool", description = "A test tool", target = "test_screen")
    fun testTool(
        @AcpParam(description = "A test param") query: String,
        @AcpParam(description = "Custom name", name = "custom_id") itemId: String
    ) {}
}

class AnnotationsTest {

    @Test
    fun `AcpToolHost is available at runtime`() {
        val annotation = SampleToolHost::class.findAnnotation<AcpToolHost>()
        assertNotNull(annotation)
    }

    @Test
    fun `AcpTool is available at runtime with correct values`() {
        val function = SampleToolHost::class.functions.first { it.name == "testTool" }
        val annotation = function.findAnnotation<AcpTool>()
        assertNotNull(annotation)
        assertEquals("test_tool", annotation!!.name)
        assertEquals("A test tool", annotation.description)
        assertEquals("test_screen", annotation.target)
    }

    @Test
    fun `AcpParam is available at runtime with correct values`() {
        val function = SampleToolHost::class.functions.first { it.name == "testTool" }
        val params = function.valueParameters

        val queryParam = params[0].findAnnotation<AcpParam>()
        assertNotNull(queryParam)
        assertEquals("A test param", queryParam!!.description)
        assertEquals("", queryParam.name)

        val itemParam = params[1].findAnnotation<AcpParam>()
        assertNotNull(itemParam)
        assertEquals("Custom name", itemParam!!.description)
        assertEquals("custom_id", itemParam.name)
    }

    @Test
    fun `AcpParam name defaults to empty string`() {
        val function = SampleToolHost::class.functions.first { it.name == "testTool" }
        val queryParam = function.valueParameters[0].findAnnotation<AcpParam>()!!
        assertEquals("", queryParam.name)
    }

    @Test
    fun `AcpToolHandler interface is implementable`() {
        val handler = object : AcpToolHandler {
            override fun onToolCall(name: String, params: Map<String, Any?>): Map<String, Any?>? {
                return if (name == "test") mapOf("result" to "ok") else null
            }
        }

        assertEquals(mapOf("result" to "ok"), handler.onToolCall("test", emptyMap()))
        assertNull(handler.onToolCall("unknown", emptyMap()))
    }
}
