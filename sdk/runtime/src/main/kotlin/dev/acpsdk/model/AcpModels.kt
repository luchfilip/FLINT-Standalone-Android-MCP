package dev.acpsdk.model

import kotlinx.serialization.Serializable

@Serializable
data class AcpScreenSnapshot(
    val screen: String,
    val content: AcpContent,
    val overlays: List<AcpOverlay> = emptyList()
)

@Serializable
data class AcpContent(
    val elements: List<AcpElement> = emptyList()
)

@Serializable
sealed class AcpElement {
    @Serializable
    data class ListElement(
        val type: String = "list",
        val id: String,
        val description: String = "",
        val items: List<AcpListItem> = emptyList()
    ) : AcpElement()

    @Serializable
    data class ContentElement(
        val type: String = "content",
        val key: String,
        val value: String
    ) : AcpElement()

    @Serializable
    data class ActionElement(
        val type: String = "action",
        val name: String,
        val description: String = ""
    ) : AcpElement()
}

@Serializable
data class AcpListItem(
    val index: Int,
    val content: Map<String, String> = emptyMap(),
    val actions: List<AcpAction> = emptyList()
)

@Serializable
data class AcpAction(
    val name: String,
    val description: String = ""
)

@Serializable
data class AcpOverlay(
    val id: String,
    val description: String = "",
    val content: Map<String, String> = emptyMap(),
    val actions: List<AcpAction> = emptyList()
)
