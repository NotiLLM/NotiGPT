package org.muilab.notigpt.model.notifications

data class NotiActions(
    var pinned: Boolean = false
) {
    fun flipPin() {
        pinned = !pinned
    }
}
