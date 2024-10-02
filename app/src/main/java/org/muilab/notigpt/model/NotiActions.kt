package org.muilab.notigpt.model

data class NotiActions(
    var pinned: Boolean = false
) {
    fun flipPin() {
        pinned = !pinned
    }
}
