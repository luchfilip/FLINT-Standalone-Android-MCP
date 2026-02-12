package dev.mcphub.acp.provider

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getAllSemanticsNodes
import dev.mcphub.acp.Acp
import dev.mcphub.acp.model.*
import dev.mcphub.acp.semantics.AcpSemantic

/**
 * Walks the Compose semantic tree to produce structured AcpScreenSnapshot.
 */
internal object AcpTreeWalker {

    /** Safe getter — returns null if key is not present. */
    private fun <T> SemanticsConfiguration.getOrNull(key: SemanticsPropertyKey<T>): T? {
        return if (contains(key)) this[key] else null
    }

    fun snapshot(): AcpScreenSnapshot {
        val screenName = Acp.getScreenName() ?: "unknown"
        val activity = getCurrentActivity() ?: return AcpScreenSnapshot(
            screen = screenName,
            content = AcpContent()
        )

        val nodes = collectSemanticNodes(activity)
        return buildSnapshot(screenName, nodes)
    }

    private fun getCurrentActivity(): Activity? {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
            val activitiesField = activityThread.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val activities = activitiesField.get(currentActivityThread) as? android.util.ArrayMap<*, *>
            activities?.values?.firstOrNull()?.let { record ->
                val activityField = record.javaClass.getDeclaredField("activity")
                activityField.isAccessible = true
                activityField.get(record) as? Activity
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun collectSemanticNodes(activity: Activity): List<SemanticsNode> {
        val rootView = activity.window?.decorView ?: return emptyList()
        val composeViews = findComposeViews(rootView)

        return composeViews.flatMap { composeView ->
            try {
                val ownerField = composeView.javaClass.getDeclaredField("semanticsOwner")
                ownerField.isAccessible = true
                val owner = ownerField.get(composeView) as? SemanticsOwner
                owner?.getAllSemanticsNodes(mergingEnabled = false) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun findComposeViews(view: View): List<View> {
        val result = mutableListOf<View>()
        if (view.javaClass.name.contains("ComposeView") ||
            view.javaClass.name.contains("AndroidComposeView")) {
            result.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                result.addAll(findComposeViews(view.getChildAt(i)))
            }
        }
        return result
    }

    private fun buildSnapshot(screen: String, nodes: List<SemanticsNode>): AcpScreenSnapshot {
        val elements = mutableListOf<AcpElement>()
        val overlays = mutableListOf<AcpOverlay>()
        val listNodes = mutableMapOf<String, MutableList<SemanticsNode>>()
        val listDescriptions = mutableMapOf<String, String>()

        for (node in nodes) {
            val config = node.config

            val overlayId = config.getOrNull(AcpSemantic.OverlayId)
            if (overlayId != null) {
                val overlayDesc = config.getOrNull(AcpSemantic.OverlayDescription) ?: ""
                val overlayContent = mutableMapOf<String, String>()
                val overlayActions = mutableListOf<AcpAction>()
                collectContents(node, overlayContent, overlayActions)
                overlays.add(AcpOverlay(id = overlayId, description = overlayDesc, content = overlayContent, actions = overlayActions))
                continue
            }

            val listId = config.getOrNull(AcpSemantic.ListId)
            if (listId != null) {
                listDescriptions[listId] = config.getOrNull(AcpSemantic.ListDescription) ?: ""
                if (listNodes[listId] == null) listNodes[listId] = mutableListOf()
                continue
            }

            val itemIndex = config.getOrNull(AcpSemantic.ItemIndex)
            if (itemIndex != null) {
                val parentListId = findParentListId(node, nodes)
                if (parentListId != null) {
                    listNodes.getOrPut(parentListId) { mutableListOf() }.add(node)
                }
                continue
            }

            val contentKey = config.getOrNull(AcpSemantic.ContentKey)
            if (contentKey != null) {
                elements.add(AcpElement.ContentElement(key = contentKey, value = extractText(node)))
                continue
            }

            val actionName = config.getOrNull(AcpSemantic.ActionName)
            if (actionName != null) {
                val actionDesc = config.getOrNull(AcpSemantic.ActionDescription) ?: ""
                elements.add(AcpElement.ActionElement(name = actionName, description = actionDesc))
            }
        }

        for ((listId, itemNodeList) in listNodes) {
            val items = itemNodeList.mapNotNull { itemNode ->
                val index = itemNode.config.getOrNull(AcpSemantic.ItemIndex) ?: return@mapNotNull null
                val content = mutableMapOf<String, String>()
                val actions = mutableListOf<AcpAction>()
                collectContents(itemNode, content, actions)
                AcpListItem(index = index, content = content, actions = actions)
            }.sortedBy { it.index }

            elements.add(AcpElement.ListElement(
                id = listId,
                description = listDescriptions[listId] ?: "",
                items = items
            ))
        }

        return AcpScreenSnapshot(screen = screen, content = AcpContent(elements = elements), overlays = overlays)
    }

    private fun findParentListId(node: SemanticsNode, allNodes: List<SemanticsNode>): String? {
        var parent = node.parent
        while (parent != null) {
            val listId = parent.config.getOrNull(AcpSemantic.ListId)
            if (listId != null) return listId
            parent = parent.parent
        }
        return allNodes.firstOrNull { it.config.getOrNull(AcpSemantic.ListId) != null }
            ?.config?.getOrNull(AcpSemantic.ListId)
    }

    private fun collectContents(
        node: SemanticsNode,
        content: MutableMap<String, String>,
        actions: MutableList<AcpAction>
    ) {
        val actionName = node.config.getOrNull(AcpSemantic.ActionName)
        if (actionName != null) {
            val actionDesc = node.config.getOrNull(AcpSemantic.ActionDescription) ?: ""
            actions.add(AcpAction(name = actionName, description = actionDesc))
        }

        for (child in node.children) {
            val contentKey = child.config.getOrNull(AcpSemantic.ContentKey)
            if (contentKey != null) {
                content[contentKey] = extractText(child)
            }
            collectContents(child, content, actions)
        }
    }

    private fun extractText(node: SemanticsNode): String {
        return try {
            val textEntry = node.config.firstOrNull { it.key.name == "Text" }
            @Suppress("UNCHECKED_CAST")
            val textList = textEntry?.value as? List<*>
            textList?.firstOrNull()?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
