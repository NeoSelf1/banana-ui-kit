package com.neon.core.ui.component.loadingState

/**
 * Represents loading state for a collection of items so each item can show its own spinner.
 * Mirrors the SwiftUI `ItemLoadingState` helper used on iOS.
 */
data class ItemLoadingState<ID>(
    private val loadingIds: Set<ID> = emptySet()
) {
    fun startLoading(id: ID): ItemLoadingState<ID> = copy(loadingIds = loadingIds + id)

    fun finishLoading(id: ID): ItemLoadingState<ID> = copy(loadingIds = loadingIds - id)

    fun isLoading(id: ID): Boolean = loadingIds.contains(id)

    companion object {
        fun <ID> idle(): ItemLoadingState<ID> = ItemLoadingState()
    }
}
