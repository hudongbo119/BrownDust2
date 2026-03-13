package com.example.browndustbot

import android.graphics.Rect

enum class MatchType {
    IMAGE,
    TEXT,
    IMAGE_OR_TEXT,
    IMAGE_AND_TEXT
}

enum class ActionType {
    CLICK,
    LONG_CLICK,
    SWIPE,
    WAIT,
    WAIT_DISAPPEAR,
    READ_TEXT,
    CONDITION_CHECK
}

enum class TextMatchMode {
    EXACT,
    CONTAINS,
    REGEX,
    STARTS_WITH,
    ENDS_WITH
}

data class TextMatchConfig(
    val targetText: String,
    val matchMode: TextMatchMode = TextMatchMode.CONTAINS,
    val useChinese: Boolean = true,
    val searchRegion: SerializableRect? = null,
    val clickOnText: Boolean = true
)

data class SerializableRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun toRect(): Rect = Rect(left, top, right, bottom)

    companion object {
        fun fromRect(rect: Rect) = SerializableRect(rect.left, rect.top, rect.right, rect.bottom)
    }
}

data class TaskStep(
    val name: String,
    val matchType: MatchType = MatchType.IMAGE,
    val templateImagePath: String? = null,
    val imageThreshold: Double = 0.85,
    val textConfig: TextMatchConfig? = null,
    val action: ActionType = ActionType.CLICK,
    val preDelayMs: Long = 500,
    val postDelayMs: Long = 500,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val swipeEndX: Int = 0,
    val swipeEndY: Int = 0,
    val swipeDurationMs: Long = 300,
    val longClickDurationMs: Long = 1000,
    val waitTimeoutMs: Long = 10000,
    val optional: Boolean = false,
    val conditionRegion: SerializableRect? = null,
    val conditionRegex: String? = null,
    val conditionMinValue: Int? = null,
    val variableName: String? = null
)

data class TaskConfig(
    val name: String,
    val targetPackage: String = "",
    val steps: List<TaskStep> = emptyList(),
    val loopCount: Int = 1,
    val loopDelayMs: Long = 1000
)
