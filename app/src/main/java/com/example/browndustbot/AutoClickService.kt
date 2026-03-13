package com.example.browndustbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoClickService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClickService"
        var instance: AutoClickService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AutoClickService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            TaskEngine.instance?.onForegroundAppChanged(packageName)
            Log.d(TAG, "Foreground app changed to: $packageName")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoClickService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "AutoClickService destroyed")
    }

    fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 50)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(strokeDescription)
            .build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d(TAG, "Click completed at ($x, $y)")
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.w(TAG, "Click cancelled at ($x, $y)")
            }
        }, null)
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(strokeDescription)
            .build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d(TAG, "Swipe completed from ($startX, $startY) to ($endX, $endY)")
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.w(TAG, "Swipe cancelled")
            }
        }, null)
    }

    fun performLongClick(x: Float, y: Float, duration: Long = 1000) {
        val path = Path().apply { moveTo(x, y) }
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(strokeDescription)
            .build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d(TAG, "Long click completed at ($x, $y)")
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.w(TAG, "Long click cancelled")
            }
        }, null)
    }
}
