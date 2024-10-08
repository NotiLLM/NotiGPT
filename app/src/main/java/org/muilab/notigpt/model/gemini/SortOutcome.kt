package org.muilab.notigpt.model.gemini

data class SortOutcome(
    val id: Int,
    val timeSensitiveness: Double,
    val senderAttractiveness: Double,
    val contentAttractiveness: Double
)
