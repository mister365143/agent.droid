package dev.agentdroid.app.screen

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Typed, read-only view of the screen at one moment.
 *
 * This is the whole surface the rest of the app gets from the accessibility
 * tree: no node handles, no actions, nothing that can be replayed later. Text is
 * data — see ADR 0001 — so callers must never treat it as instructions.
 */
data class ScreenSnapshot(
    val packageName: String?,
    val capturedAtMillis: Long,
    val nodes: List<ScreenNode>,
    val truncated: Boolean,
)

/** One element of the accessibility tree, flattened. */
data class ScreenNode(
    val depth: Int,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val bounds: Bounds,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
)

/** Screen-space bounds in pixels, as reported by the platform. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Bounds on what a single read costs: a hostile or enormous screen can produce a
 * tree with tens of thousands of nodes, so traversal is capped rather than
 * unbounded.
 */
object ScreenTreeLimits {
    const val MAX_NODES = 400
    const val MAX_DEPTH = 24
}

/**
 * Flattens an accessibility tree into a [ScreenSnapshot], breadth first so the
 * nodes nearest the top of the screen survive when the cap is hit. Returns null
 * when there is nothing readable.
 */
internal fun snapshotOf(
    root: AccessibilityNodeInfo?,
    nowMillis: Long = System.currentTimeMillis(),
    maxNodes: Int = ScreenTreeLimits.MAX_NODES,
): ScreenSnapshot? {
    if (root == null) return null

    val nodes = ArrayList<ScreenNode>(minOf(maxNodes, 128))
    val pending = ArrayDeque<NodeAtDepth>()
    var truncated = false

    pending.addLast(NodeAtDepth(root, 0))
    while (pending.isNotEmpty()) {
        if (nodes.size >= maxNodes) {
            truncated = true
            break
        }
        val (node, depth) = pending.removeFirst()
        nodes += node.toScreenNode(depth)

        if (depth >= ScreenTreeLimits.MAX_DEPTH) {
            if (node.childCount > 0) truncated = true
            continue
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            pending.addLast(NodeAtDepth(child, depth + 1))
        }
    }

    return ScreenSnapshot(
        packageName = root.packageName?.toString(),
        capturedAtMillis = nowMillis,
        nodes = nodes,
        truncated = truncated,
    )
}

private data class NodeAtDepth(val node: AccessibilityNodeInfo, val depth: Int)

private fun AccessibilityNodeInfo.toScreenNode(depth: Int): ScreenNode {
    val rect = Rect()
    getBoundsInScreen(rect)
    return ScreenNode(
        depth = depth,
        className = className?.toString(),
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        viewId = viewIdResourceName,
        bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
        clickable = isClickable,
        editable = isEditable,
        scrollable = isScrollable,
    )
}
