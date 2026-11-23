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

/** One labelled element of the accessibility tree. */
data class ScreenNode(
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val bounds: Bounds,
    val clickable: Boolean,
)

/** Screen-space bounds in pixels, as reported by the platform. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

// A hostile or enormous screen can produce a tree with tens of thousands of
// nodes, so traversal is capped rather than unbounded.
private const val MAX_NODES = 400
private const val MAX_DEPTH = 24

/**
 * Flattens an accessibility tree into a [ScreenSnapshot], breadth first so the
 * nodes nearest the top of the screen survive when the cap is hit. Returns null
 * when there is nothing readable.
 */
internal fun snapshotOf(
    root: AccessibilityNodeInfo?,
    nowMillis: Long = System.currentTimeMillis(),
    maxNodes: Int = MAX_NODES,
): ScreenSnapshot? {
    if (root == null) return null

    val nodes = ArrayList<ScreenNode>(minOf(maxNodes, 128))
    val pending = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
    var truncated = false

    pending.addLast(root to 0)
    while (pending.isNotEmpty()) {
        if (nodes.size >= maxNodes) {
            truncated = true
            break
        }
        val (node, depth) = pending.removeFirst()
        nodes += node.toScreenNode()

        if (depth >= MAX_DEPTH) {
            if (node.childCount > 0) truncated = true
            continue
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            pending.addLast(child to depth + 1)
        }
    }

    return ScreenSnapshot(
        packageName = root.packageName?.toString(),
        capturedAtMillis = nowMillis,
        nodes = nodes,
        truncated = truncated,
    )
}

private fun AccessibilityNodeInfo.toScreenNode(): ScreenNode {
    val rect = Rect()
    getBoundsInScreen(rect)
    return ScreenNode(
        className = className?.toString(),
        text = text?.toString(),
        contentDescription = contentDescription?.toString(),
        bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
        clickable = isClickable,
    )
}
