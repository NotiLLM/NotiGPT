package org.muilab.notigpt.model.notifications

data class NotiFeature(
    val featureString: String
) {

    companion object {

        fun makeFeatureString(): String {
            return ""
        }
    }
    constructor(): this (
        featureString = makeFeatureString()
    )
}
