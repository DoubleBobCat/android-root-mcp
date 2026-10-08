package com.doublecat.android_root_mcp

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Path
import android.accessibilityservice.GestureDescription
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * UI hierarchy backend which does not create a UiAutomation session.
 * This avoids competing with the device harness' singleton UiAutomation.
 */
class ArmcpAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        eventGeneration.incrementAndGet()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** Returns the freshest complete active-window snapshot available. */
    fun snapshot(): UiSnapshot? {
        var latestSnapshot: UiSnapshot? = null
        repeat(MAX_SNAPSHOT_ATTEMPTS) {
            val generationBefore = eventGeneration.get()
            val windows = try { getWindows() } catch (_: Exception) { emptyList() }
            val activeWindow = windows.firstOrNull { it.isActive }
                ?: windows.firstOrNull { it.isFocused }
            val root = try { activeWindow?.root ?: rootInActiveWindow } catch (_: Exception) { null }
            try {
                if (root == null) return@repeat
                val snapshot = UiSnapshotBuilder().build(root)
                latestSnapshot = snapshot
                if (generationBefore == eventGeneration.get()) return snapshot
                // Dynamic applications may update continuously. Keep the latest
                // complete tree rather than falling back to a competing UiAutomation.
            } catch (_: Exception) {
                // A remote application can invalidate a node during traversal.
                // Retry with a fresh active-window root.
            } finally {
                root?.recycle()
                windows.forEach { it.recycle() }
            }
        }
        return latestSnapshot
    }

    fun dispatchTap(x: Int, y: Int): Boolean = dispatchPath { path ->
        path.moveTo(x.toFloat(), y.toFloat())
        path.lineTo(x.toFloat(), y.toFloat())
    }

    fun dispatchSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): Boolean {
        return dispatchPath(durationMs) { path ->
            path.moveTo(x1.toFloat(), y1.toFloat())
            path.lineTo(x2.toFloat(), y2.toFloat())
        }
    }

    fun setFocusedText(text: String): Boolean {
        val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return false
        val node = try { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } catch (_: Exception) { null }
            ?: return false
        return try {
            node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                },
            )
        } finally {
            node.recycle()
            root.recycle()
        }
    }

    private fun dispatchPath(durationMs: Long = 300L, builder: (Path) -> Unit): Boolean {
        val path = Path()
        builder(path)
        val latch = CountDownLatch(1)
        var completed = false
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                completed = true
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                latch.countDown()
            }
        }, null)
        if (!accepted) return false
        latch.await(2, TimeUnit.SECONDS)
        return completed
    }

    companion object {
        @Volatile
        var instance: ArmcpAccessibilityService? = null

        fun awaitInstance(timeoutMs: Long = 1_500L): ArmcpAccessibilityService? {
            val deadline = System.nanoTime() + timeoutMs * 1_000_000L
            while (System.nanoTime() < deadline) {
                instance?.let { return it }
                try {
                    Thread.sleep(50L)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
            return instance
        }

        private const val MAX_SNAPSHOT_ATTEMPTS = 3
        private val eventGeneration = AtomicLong(0L)
    }
}

private class UiSnapshotBuilder {
    private val output = StringBuilder()

    fun build(root: AccessibilityNodeInfo): UiSnapshot {
        output.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n")
        output.append("<hierarchy rotation=\"0\">\n")
        appendNode(root, 1, 0)
        output.append("</hierarchy>")
        return UiSnapshot(output.toString())
    }

    private fun appendNode(node: AccessibilityNodeInfo, depth: Int, index: Int) {
        // Refresh is best-effort: some applications expose readable nodes that
        // cannot be refreshed while their window is changing.
        runCatching { node.refresh() }
        repeat(depth) { output.append("  ") }
        output.append("<node")
        attribute("index", index.toString())
        attribute("text", node.text?.toString())
        attribute("resource-id", node.viewIdResourceName)
        attribute("class", node.className?.toString())
        attribute("package", node.packageName?.toString())
        attribute("content-desc", node.contentDescription?.toString())
        attribute("checkable", node.isCheckable.toString())
        attribute("checked", node.isChecked.toString())
        attribute("clickable", node.isClickable.toString())
        attribute("enabled", node.isEnabled.toString())
        attribute("focusable", node.isFocusable.toString())
        attribute("focused", node.isFocused.toString())
        attribute("scrollable", node.isScrollable.toString())
        attribute("selected", node.isSelected.toString())
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        attribute("bounds", "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]")

        if (node.childCount == 0) {
            output.append(" />\n")
            return
        }
        output.append(">\n")
        for (childIndex in 0 until node.childCount) {
            val child = try { node.getChild(childIndex) } catch (_: Exception) { null }
                ?: continue
            try {
                appendNode(child, depth + 1, childIndex)
            } finally {
                child.recycle()
            }
        }
        repeat(depth) { output.append("  ") }
        output.append("</node>\n")
    }

    private fun attribute(name: String, value: String?) {
        if (value == null) return
        output.append(' ').append(name).append("=\"")
            .append(escape(value))
            .append("\"")
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
