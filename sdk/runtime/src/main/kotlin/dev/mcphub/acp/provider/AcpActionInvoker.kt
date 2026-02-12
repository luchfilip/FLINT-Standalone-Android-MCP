package dev.mcphub.acp.provider

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getAllSemanticsNodes
import dev.mcphub.acp.semantics.AcpSemantic

/**
 * Finds and invokes semantic actions by name.
 */
internal object AcpActionInvoker {

    private fun <T> SemanticsConfiguration.getOrNull(key: SemanticsPropertyKey<T>): T? {
        return if (contains(key)) this[key] else null
    }

    fun invoke(actionName: String, listId: String? = null, itemIndex: Int? = null): Boolean {
        val activity = getCurrentActivity() ?: return false
        val nodes = collectAllSemanticNodes(activity)
        val targetNode = findActionNode(nodes, actionName, listId, itemIndex) ?: return false

        return try {
            val onClick = targetNode.config.getOrNull(SemanticsActions.OnClick)
            if (onClick != null) {
                onClick.action?.invoke()
                true
            } else {
                performAccessibilityClick(activity, targetNode)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun findActionNode(
        nodes: List<SemanticsNode>,
        actionName: String,
        listId: String?,
        itemIndex: Int?
    ): SemanticsNode? {
        for (node in nodes) {
            val nodeActionName = node.config.getOrNull(AcpSemantic.ActionName) ?: continue
            if (nodeActionName != actionName) continue

            if (listId != null && itemIndex != null) {
                val nodeItemIndex = node.config.getOrNull(AcpSemantic.ItemIndex) ?: findParentItemIndex(node)
                if (nodeItemIndex != itemIndex) continue
                val nodeListId = findParentListId(node)
                if (nodeListId != listId) continue
            }
            return node
        }
        return null
    }

    private fun findParentItemIndex(node: SemanticsNode): Int? {
        var parent = node.parent
        while (parent != null) {
            val idx = parent.config.getOrNull(AcpSemantic.ItemIndex)
            if (idx != null) return idx
            parent = parent.parent
        }
        return null
    }

    private fun findParentListId(node: SemanticsNode): String? {
        var parent = node.parent
        while (parent != null) {
            val id = parent.config.getOrNull(AcpSemantic.ListId)
            if (id != null) return id
            parent = parent.parent
        }
        return null
    }

    private fun performAccessibilityClick(activity: Activity, node: SemanticsNode): Boolean {
        return try {
            val bounds = node.boundsInWindow
            val centerX = (bounds.left + bounds.right) / 2
            val centerY = (bounds.top + bounds.bottom) / 2
            val rootView = activity.window?.decorView ?: return false

            val downEvent = android.view.MotionEvent.obtain(
                System.currentTimeMillis(), System.currentTimeMillis(),
                android.view.MotionEvent.ACTION_DOWN, centerX, centerY, 0
            )
            rootView.dispatchTouchEvent(downEvent)
            downEvent.recycle()

            val upEvent = android.view.MotionEvent.obtain(
                System.currentTimeMillis(), System.currentTimeMillis(),
                android.view.MotionEvent.ACTION_UP, centerX, centerY, 0
            )
            rootView.dispatchTouchEvent(upEvent)
            upEvent.recycle()
            true
        } catch (e: Exception) {
            false
        }
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

    private fun collectAllSemanticNodes(activity: Activity): List<SemanticsNode> {
        val rootView = activity.window?.decorView ?: return emptyList()
        return findComposeViews(rootView).flatMap { composeView ->
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
}
