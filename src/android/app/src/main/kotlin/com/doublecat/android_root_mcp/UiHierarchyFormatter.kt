package com.doublecat.android_root_mcp

import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.LinkedHashMap

/** Formats UI inspection snapshots as readable documents for MCP content items. */
internal object UiHierarchyFormatter {
    private val boundsPattern = Regex("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]")

    fun format(
        xml: String,
        filter: String,
        backend: String,
        asJson: Boolean,
    ): String {
        val root = parse(xml)
        val filteredRoot = if (filter.isEmpty()) root else filterTree(root, filter) ?: root.copy(
            attributes = LinkedHashMap(root.attributes),
            children = mutableListOf(),
        )
        addCoordinateMetadata(filteredRoot)
        return if (asJson) {
            formatJson(filteredRoot, filter, backend)
        } else {
            formatXml(filteredRoot, filter, backend)
        }
    }

    private fun parse(xml: String): UiNode {
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
        }
        while (parser.next() != XmlPullParser.START_TAG) {
            if (parser.eventType == XmlPullParser.END_DOCUMENT) {
                throw IllegalArgumentException("UI hierarchy has no root element")
            }
        }
        return parseNode(parser)
    }

    private fun parseNode(parser: XmlPullParser): UiNode {
        val attributes = LinkedHashMap<String, String>()
        for (index in 0 until parser.attributeCount) {
            attributes[parser.getAttributeName(index)] = parser.getAttributeValue(index)
        }
        val node = UiNode(parser.name, attributes)
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> node.children += parseNode(parser)
                XmlPullParser.END_TAG -> return node
                XmlPullParser.END_DOCUMENT -> throw IllegalArgumentException("UI hierarchy ended before </${node.name}>")
            }
        }
    }

    private fun addCoordinateMetadata(node: UiNode) {
        parseBounds(node.attributes["bounds"])?.let { bounds ->
            node.attributes["bounds-left"] = bounds.left.toString()
            node.attributes["bounds-top"] = bounds.top.toString()
            node.attributes["bounds-right"] = bounds.right.toString()
            node.attributes["bounds-bottom"] = bounds.bottom.toString()
            node.attributes["width"] = bounds.width.toString()
            node.attributes["height"] = bounds.height.toString()
            node.attributes["center-x"] = bounds.centerX.toString()
            node.attributes["center-y"] = bounds.centerY.toString()
            node.attributes["tap-x"] = bounds.centerX.toString()
            node.attributes["tap-y"] = bounds.centerY.toString()
        }
        node.children.forEach(::addCoordinateMetadata)
    }

    private fun filterTree(node: UiNode, filter: String): UiNode? {
        val matchingChildren = node.children.mapNotNull { filterTree(it, filter) }.toMutableList()
        val matches = node.attributes.values.any { it.contains(filter, ignoreCase = true) }
        if (!matches && matchingChildren.isEmpty()) return null
        return node.copy(
            attributes = LinkedHashMap(node.attributes),
            children = matchingChildren,
        )
    }

    private fun formatXml(root: UiNode, filter: String, backend: String): String {
        val attributes = LinkedHashMap(root.attributes).apply {
            put("format", "xml")
            put("backend", backend)
            put("filter", filter)
            put("coordinate-space", "screen_pixels")
            put("coordinate-origin", "top-left")
            put("tap-coordinate", "center")
        }
        val documentRoot = root.copy(attributes = attributes)
        val output = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            appendXml(documentRoot, 0, this)
        }
        return output
    }

    private fun appendXml(node: UiNode, depth: Int, output: StringBuilder) {
        repeat(depth) { output.append("  ") }
        output.append('<').append(node.name)
        node.attributes.forEach { (name, value) ->
            output.append(' ').append(name).append("=\"").append(escapeXml(value)).append("\"")
        }
        if (node.children.isEmpty()) {
            output.append(" />\n")
            return
        }
        output.append(">\n")
        node.children.forEach { appendXml(it, depth + 1, output) }
        repeat(depth) { output.append("  ") }
        output.append("</").append(node.name).append(">\n")
    }

    private fun formatJson(root: UiNode, filter: String, backend: String): String {
        val document = JSONObject()
            .put("format", "json")
            .put("backend", backend)
            .put("filter", filter)
            .put("coordinateSystem", JSONObject()
                .put("space", "screen_pixels")
                .put("origin", "top-left")
                .put("xAxis", "right")
                .put("yAxis", "down")
                .put("tapPoint", "center of bounds"))
            .put("hierarchy", nodeToJson(root))
        val output = document.toString(2)
        return output
    }

    private fun nodeToJson(node: UiNode): JSONObject {
        val output = JSONObject()
            .put("element", node.name)
            .put("attributes", JSONObject().apply {
                node.attributes.forEach { (name, value) -> put(name, value) }
            })
        parseBounds(node.attributes["bounds"])?.let { bounds ->
            output.put("bounds", JSONObject()
                .put("left", bounds.left)
                .put("top", bounds.top)
                .put("right", bounds.right)
                .put("bottom", bounds.bottom))
                .put("size", JSONObject()
                    .put("width", bounds.width)
                    .put("height", bounds.height))
                .put("tap", JSONObject()
                    .put("x", bounds.centerX)
                    .put("y", bounds.centerY)
                    .put("point", "center"))
        }
        if (node.children.isNotEmpty()) {
            output.put("children", JSONArray().apply { node.children.forEach { put(nodeToJson(it)) } })
        }
        return output
    }

    private fun parseBounds(value: String?): Bounds? {
        val match = boundsPattern.matchEntire(value ?: "") ?: return null
        return Bounds(
            left = match.groupValues[1].toInt(),
            top = match.groupValues[2].toInt(),
            right = match.groupValues[3].toInt(),
            bottom = match.groupValues[4].toInt(),
        )
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private data class UiNode(
        val name: String,
        val attributes: LinkedHashMap<String, String>,
        val children: MutableList<UiNode> = mutableListOf(),
    )

    private data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        val centerX: Int get() = left + width / 2
        val centerY: Int get() = top + height / 2
    }
}
