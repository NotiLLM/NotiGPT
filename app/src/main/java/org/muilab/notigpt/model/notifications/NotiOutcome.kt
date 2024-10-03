package org.muilab.notigpt.model.notifications

data class NotiOutcome(
    var score: Double = 100.0,
    var summary: String = ""
) {
    fun resetOutcomes() {
        summary = ""
        score = 30.0
    }
}
