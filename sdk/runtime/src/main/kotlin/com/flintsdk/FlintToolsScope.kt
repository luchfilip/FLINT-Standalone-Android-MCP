package com.flintsdk

import com.flintsdk.annotations.FlintParamDescriptor
import com.flintsdk.annotations.FlintToolDescriptor
import com.flintsdk.annotations.FlintToolHandler

class FlintToolsScope {

    private val descriptors = mutableListOf<FlintToolDescriptor>()
    private val actions = mutableMapOf<String, (Map<String, Any?>) -> Map<String, Any?>?>()

    /** Define a tool via builder DSL. Use param() for parameters, action() for the handler. */
    fun tool(name: String, description: String, block: ToolBuilder.() -> Unit) {
        val builder = ToolBuilder().apply(block)
        descriptors.add(FlintToolDescriptor(name, description, builder.params))
        val toolAction = builder.actionFn ?: error("tool '$name' must define an action {} block")
        actions[name] = { params ->
            toolAction(params) ?: mapOf("_ok" to true)
        }
    }

    fun buildHandler(): FlintToolHandler = object : FlintToolHandler {
        private val descs = descriptors.toList()
        private val acts = actions.toMap()

        override fun onToolCall(name: String, params: Map<String, Any?>): Map<String, Any?>? {
            val fn = acts[name] ?: return null
            return fn(params)
        }

        override fun describeTools(): List<FlintToolDescriptor> = descs
    }

    class ToolBuilder {
        internal val params = mutableListOf<FlintParamDescriptor>()
        internal var actionFn: ((Map<String, Any?>) -> Map<String, Any?>?)? = null

        fun param(name: String, type: String, description: String, required: Boolean = true) {
            params.add(FlintParamDescriptor(name, type, description, required))
        }

        fun action(block: (Map<String, Any?>) -> Map<String, Any?>?) {
            actionFn = block
        }
    }
}
