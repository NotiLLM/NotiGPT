package org.muilab.notigpt.model

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
