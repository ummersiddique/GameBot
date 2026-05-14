package com.game.bot

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class GameBotService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isActionPending = false
    private var currentActivityName: CharSequence? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentActivityName = event.className
        }

        val rootNode = rootInActiveWindow ?: return

        // If we are waiting for a click to execute, don't do anything else
        if (isActionPending) return

        // Strictly restrict to Facebook Audience Network Activity
        // (Also allowing MainActivity for your local testing)
        val isTargetActivity = currentActivityName?.contains("AudienceNetworkActivity") == true ||
                       currentActivityName?.contains("MainActivity") == true

        if (!isTargetActivity) return

        val targetNode = findTargetNode(rootNode)
        val hasProgressBar = !isProgressFinished(rootNode)

        if (hasProgressBar) {
            Log.e("GameBotService", "Waiting for progress to complete")
        } else if (targetNode != null) {
            Log.e("GameBotService", "Progress completed - Target found: ${targetNode.text ?: targetNode.contentDescription}")
            performDelayedClick(targetNode)
        }
    }

    private fun findTargetNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for buttons that give rewards or close ads
        val textButtons = listOf("Claim", "Collect", "Reward", "Skip Ad", "Skip", "Finish", "Next")
        for (text in textButtons) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) return node
            }
        }

        // Look for content descriptions (X buttons often have these)
        val descriptions = listOf("Close Ad", "Close", "Dismiss", "Skip")
        for (desc in descriptions) {
            val node = findNodeByDescription(rootNode, desc)
            if (node != null) return node
        }

        // Facebook Ads fallback (Clickable container with ImageView)
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
        // Start from the end as ad close buttons are usually late in the hierarchy
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "game_bot_channel",
                "Game Bot Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "game_bot_channel")
            .setContentTitle("Game Bot is Running")
            .setContentText("Monitoring for rewards...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    override fun onInterrupt() {
        Log.e("GameBotService", "Service Interrupted")
        handler.removeCallbacksAndMessages(null)
        isActionPending = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e("GameBotService", "Service Connected and Foregrounded")
    }
}
