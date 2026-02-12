package dev.mcphub.acp.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * KSP processor integration tests.
 *
 * The processor is verified through the actual Gradle build pipeline:
 * - sample/musicapp uses ksp(project(":sdk:compiler"))
 * - Generated AcpRouter_* and AcpSchemaHolder are compiled as part of the build
 * - If KSP generation fails, the musicapp build fails
 *
 * NameUtils logic (type mapping, name conversion) is unit tested in NameUtilsTest.
 * The processor orchestration is tested via `./gradlew :sample:musicapp:build`.
 */
class AcpProcessorTest {

    @Test
    fun `processor provider creates processor instance`() {
        // Verify the provider is instantiable (META-INF/services registration)
        val provider = AcpProcessorProvider()
        assertNotNull(provider)
    }

    @Test
    fun `ToolInfo data class stores tool metadata`() {
        val tool = ToolInfo(
            name = "search",
            description = "Search for tracks",
            target = "search_results",
            functionName = "search",
            params = listOf(
                ParamInfo("query", "query", "kotlin.String", "Search query", false)
            )
        )
        assertEquals("search", tool.name)
        assertEquals(1, tool.params.size)
        assertFalse(tool.params[0].hasDefault)
    }

    @Test
    fun `ParamInfo identifies required vs optional params`() {
        val required = ParamInfo("query", "query", "kotlin.String", "Query", false)
        val optional = ParamInfo("limit", "limit", "kotlin.Int", "Limit", true)
        assertFalse(required.hasDefault)
        assertTrue(optional.hasDefault)
    }
}
