package com.neon.core.ui.component.loadingState

/**
 * 로딩 상태를 표현하는 공통 sealed class.
 * SwiftUI LoadingState와 1:1로 대응한다.
 */
sealed class LoadingState<out T> {
    data object Idle : LoadingState<Nothing>()
    data object Loading : LoadingState<Nothing>()
    data class Loaded<T>(val loadedData: T) : LoadingState<T>()
    data class Refreshing<T>(val existingData: T) : LoadingState<T>()

    val data: T?
        get() = when (this) {
            is Loaded -> loadedData
            is Refreshing -> existingData
            else -> null
        }

    val isLoading: Boolean
        get() = this is Loading

    val isRefreshing: Boolean
        get() = this is Refreshing
}

/**
 * 로딩 시작 시 기존 데이터가 있으면 Refreshing, 없으면 Loading으로 전환
 */
fun <T> LoadingState<T>.startLoading(): LoadingState<T> {
    return when (this) {
        is LoadingState.Loaded -> LoadingState.Refreshing(this.loadedData)
        is LoadingState.Refreshing -> this
        else -> LoadingState.Loading
    }
}

/**
 * 실패 처리: 기존 데이터가 있으면 유지, 없으면 Idle로 전환
 */
fun <T> LoadingState<T>.handleFailure(): LoadingState<T> {
    return when (this) {
        is LoadingState.Loaded -> this
        is LoadingState.Refreshing -> LoadingState.Loaded(this.existingData)
        else -> LoadingState.Idle
    }
}