package com.game.bot

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.random.Random

class GameBotService : AccessibilityService() {

    private var currentActivityName: CharSequence? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isActionPending = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return

        // Update current activity if the event tells us
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentActivityName = event.className
        }

        val packageName = rootNode.packageName?.toString() ?: ""
        
        // Robust target detection:
        // 1. Check if the package is ours (for testing)
        // 2. Check if the activity name contains AudienceNetworkActivity
        // 3. Check if the event's package name belongs to a known ad provider (optional)
        val isTarget = currentActivityName?.contains("AudienceNetworkActivity") == true || 
                       currentActivityName?.contains("MainActivity") == true ||
                       packageName == "com.game.bot"

        if (!isTarget) return
        if (isActionPending) return

        if (isProgressFinished(rootNode)) {
            val targetNode = findTargetNode(rootNode)
            if (targetNode != null) {
                Log.e("GameBotService", "Progress completed")
                performDelayedClick(targetNode)
            }
        } else {
            // Only log this occasionally or on specific events to avoid log flooding
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                Log.e("GameBotService", "Waiting for progress to complete")
            }
        }
    }

    private fun findTargetNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val textButtons = listOf("Next", "Continue", "Finish")
        for (text in textButtons) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) return node
            }
        }

        // Search by content description
        val closeAdNode = findNodeByDescription(rootNode, "Close Ad")
        if (closeAdNode != null) return closeAdNode

        return findFacebookCloseButton(rootNode)
    }

    private fun findNodeByDescription(node: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        val nodeDesc = node.contentDescription?.toString()
        if (nodeDesc?.contains(description, ignoreCase = true) == true) {
            if (node.isClickable && node.isEnabled) return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByDescription(child, description)
            if (found != null) return found
        }
        return null
    }

    private fun findFacebookCloseButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        fun search(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.className == "android.widget.ImageView" && !node.isClickable) {
                var parent = node.parent
                while (parent != null) {
                    if (parent.className == "android.widget.LinearLayout" && parent.isClickable) {
                        return parent
                    }
                    parent = parent.parent
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = search(child)
                if (found != null) return found
            }
            return null
        }
        for (i in rootNode.childCount - 1 downTo 0) {
            val child = rootNode.getChild(i) ?: continue
            val found = search(child)
            if (found != null) return found
        }
        return null
    }

    private fun performDelayedClick(node: AccessibilityNodeInfo) {
        val delay = Random.nextLong(1000, 2000)
        isActionPending = true
        val nodeToClick = AccessibilityNodeInfo.obtain(node)
        
        handler.postDelayed({
            try {
                if (nodeToClick.refresh() && nodeToClick.isClickable && nodeToClick.isEnabled) {
                    val name = nodeToClick.text ?: nodeToClick.contentDescription ?: "button"
                    Log.e("GameBotService", "Clicked on $name")
                    nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            } finally {
                nodeToClick.recycle()
                isActionPending = false
            }
        }, delay)
    }

    private fun isProgressFinished(node: AccessibilityNodeInfo): Boolean {
        val progressBars = findNodesByClassName(node, "android.widget.ProgressBar")
        if (progressBars.isEmpty()) return true

        for (pb in progressBars) {
            val rangeInfo = pb.rangeInfo
            if (rangeInfo != null) {
                if (rangeInfo.current < rangeInfo.max) return false
            } else {
                if (pb.isVisibleToUser) return false
            }
        }
        return true
    }

    private fun findNodesByClassName(node: AccessibilityNodeInfo, className: String): List<AccessibilityNodeInfo> {
        val foundNodes = mutableListOf<AccessibilityNodeInfo>()
        if (node.className == className) foundNodes.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            foundNodes.addAll(findNodesByClassName(child, className))
        }
        return foundNodes
    }

    override fun onInterrupt() {
        Log.e("GameBotService", "Service Interrupted")
        handler.removeCallbacksAndMessages(null)
        isActionPending = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e("GameBotService", "Service Connected")
    }
}
