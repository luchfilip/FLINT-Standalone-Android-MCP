package com.flintsdk.provider

import com.flintsdk.model.FlintElement
import com.flintsdk.model.FlintScreenSnapshot

/**
 * Renders screen state as flat LLM-friendly text.
 */
internal object FlintTextRenderer {

    fun render(screen: String, toolNames: List<String>, snapshot: FlintScreenSnapshot): String {
        return buildString {
            appendLine("screen: $screen")

            for (element in snapshot.content.elements) {
                when (element) {
                    is FlintElement.ListElement -> {
                        appendLine("${element.id}:")
                        for (item in element.items) {
                            val parts = item.content.entries.joinToString(" | ") { "${it.key}: ${it.value}" }
                            appendLine("  [${item.index}] $parts")
                        }
                    }
                    is FlintElement.ContentElement -> {
                        appendLine("${element.key}: ${element.value}")
                    }
                    is FlintElement.ActionElement -> {
                        // Actions are covered by tools list below
                    }
                }
            }

            for (overlay in snapshot.overlays) {
                appendLine("overlay(${overlay.id}):")
                for ((key, value) in overlay.content) {
                    appendLine("  $key: $value")
                }
                if (overlay.actions.isNotEmpty()) {
                    appendLine("  actions: ${overlay.actions.joinToString(", ") { it.name }}")
                }
            }

            if (toolNames.isNotEmpty()) {
                appendLine("tools: ${toolNames.joinToString(", ")}")
            }
        }.trimEnd()
    }
}
