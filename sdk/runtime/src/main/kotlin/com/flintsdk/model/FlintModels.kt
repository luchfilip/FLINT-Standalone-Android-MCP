package com.flintsdk.model

import kotlinx.serialization.Serializable

@Serializable
data class FlintScreenSnapshot(
    val screen: String,
    val content: FlintContent,
    val overlays: List<FlintOverlay> = emptyList()
)

@Serializable
data class FlintContent(
    val elements: List<FlintElement> = emptyList()
)

@Serializable
sealed class FlintElement {
    @Serializable
    data class ListElement(
        val type: String = "list",
        val id: String,
        val description: String = "",
        val items: List<FlintListItem> = emptyList()
    ) : FlintElement()

    @Serializable
    data class ContentElement(
        val type: String = "content",
        val key: String,
        val value: String
    ) : FlintElement()

    @Serializable
    data class ActionElement(
        val type: String = "action",
        val name: String,
        val description: String = ""
    ) : FlintElement()
}

@Serializable
data class FlintListItem(
    val index: Int,
    val content: Map<String, String> = emptyMap(),
    val actions: List<FlintAction> = emptyList()
)

@Serializable
data class FlintAction(
    val name: String,
    val description: String = ""
)

@Serializable
data class FlintOverlay(
    val id: String,
    val description: String = "",
    val content: Map<String, String> = emptyMap(),
    val actions: List<FlintAction> = emptyList()
)
