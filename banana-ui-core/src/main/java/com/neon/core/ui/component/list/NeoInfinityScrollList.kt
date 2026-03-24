package com.neon.core.ui.component.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neon.core.ui.component.NeoLoadingIndicator
import com.neon.core.ui.component.NeoPlaceholder
import com.neon.core.ui.component.button.NeoClickable
import com.neon.core.ui.component.loadingState.LoadingState

/**
 * Pull-to-Refresh와 무한 스크롤을 지원하는 범용 리스트 컴포넌트.
 *
 * [LoadingState]를 기반으로 로딩, 빈 상태, 데이터 표시, 새로고침 등 리스트의
 * 전체 생명주기를 자동으로 관리합니다. 내부적으로 [LazyColumn]을 사용하며,
 * 각 아이템은 [NeoClickable]로 래핑되어 탭 애니메이션이 적용됩니다.
 *
 * 주요 기능:
 * - [onRefresh] 제공 시 [PullToRefreshBox]가 활성화되어 당겨서 새로고침을 지원합니다.
 * - [onLoadMore] 제공 시 스크롤이 마지막 아이템에 도달하면 자동으로 추가 데이터를 요청합니다.
 * - 데이터가 없으면 [placeholderText]로 [com.neon.core.ui.component.NeoPlaceholder]를 표시합니다.
 * - 로딩 중에는 [com.neon.core.ui.component.NeoLoadingIndicator]를 표시합니다.
 * - [headerContent], [topContent], [footerContent]로 리스트 전후에 커스텀 영역을 배치할 수 있습니다.
 *
 * @param modifier 리스트에 적용할 Modifier.
 * @param state 리스트 데이터의 로딩 상태. [LoadingState]의 각 하위 타입에 따라 UI가 전환됩니다.
 * @param hasMore 더 로드할 데이터가 있는지 여부. true이면 하단에 로딩 인디케이터를 표시.
 * @param isLoadingMore 추가 데이터를 로딩 중인지 여부.
 * @param onTapItem 아이템 탭 시 호출되는 콜백. null이면 탭 비활성화.
 * @param onRefresh Pull-to-Refresh 시 호출되는 콜백. null이면 새로고침 비활성화.
 * @param onLoadMore 스크롤이 끝에 도달했을 때 호출되는 콜백. null이면 무한 스크롤 비활성화.
 * @param placeholderText 데이터가 비어있을 때 표시할 안내 텍스트.
 * @param tapTransition 아이템 탭 시 적용할 [NeoClickable.TransitionType]. 기본값은 ShrinkWithGrayBackground.
 * @param headerContent 리스트 최상단에 표시할 컨텐츠. 스크롤과 함께 움직인다.
 * @param topContent 리스트 아이템들 위에 표시할 컨텐츠. 로드된 데이터에 접근할 수 있습니다.
 * @param footerContent 리스트 최하단에 표시할 컨텐츠.
 * @param itemContent 각 리스트 아이템을 렌더링하는 컴포저블.
 *
 * @see LoadingState
 * @see NeoClickable
 * @see com.neon.core.ui.component.NeoPlaceholder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> NeoInfinityScrollList(
    modifier: Modifier = Modifier,
    state: LoadingState<List<T>>,
    hasMore: Boolean = false,
    isLoadingMore: Boolean? = null,
    onTapItem: ((T)-> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    placeholderText: String? = null,
    tapTransition: NeoClickable.TransitionType = NeoClickable.TransitionType.ShrinkWithGrayBackground,
    headerContent: @Composable () -> Unit = {},
    topContent: (@Composable (List<T>) -> Unit)? = null,
    footerContent: @Composable () -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    if (onRefresh != null) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = modifier
        ) {
            ScrollListContent(
                state = state,
                hasMore = hasMore,
                isLoadingMore = isLoadingMore,
                onLoadMore = onLoadMore,
                placeholderText = placeholderText,
                tapTransition = tapTransition,
                headerContent = headerContent,
                topContent = topContent,
                footerContent = footerContent,
                itemContent = itemContent,
                onTapItem = onTapItem
            )
        }
    } else {
        ScrollListContent(
            state = state,
            modifier = modifier,
            hasMore = hasMore,
            isLoadingMore = isLoadingMore,
            onLoadMore = onLoadMore,
            placeholderText = placeholderText,
            tapTransition = tapTransition,
            headerContent = headerContent,
            topContent = topContent,
            footerContent = footerContent,
            itemContent = itemContent,
            onTapItem = onTapItem
        )
    }
}

@Composable
private fun <T> ScrollListContent(
    state: LoadingState<List<T>>,
    modifier: Modifier = Modifier,
    hasMore: Boolean,
    isLoadingMore: Boolean?,
    onLoadMore: (() -> Unit)?,
    placeholderText: String?,
    tapTransition: NeoClickable.TransitionType,
    headerContent: (@Composable () -> Unit)?,
    topContent: (@Composable (List<T>) -> Unit)?,
    footerContent: (@Composable () -> Unit)?,
    itemContent: @Composable (T) -> Unit,
    onTapItem: ((T)->Unit)?
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        headerContent?.let {
            item {
                it()
            }
        }

        when {
            (state is LoadingState.Loaded && state.loadedData.isNotEmpty()) ||
            (state is LoadingState.Refreshing && state.existingData.isNotEmpty()) -> {
                val data = when (state) {
                    is LoadingState.Loaded -> state.loadedData
                    is LoadingState.Refreshing -> state.existingData
                    else -> emptyList()
                }
                topContent?.let {
                    item {
                        it(data)
                    }
                }

                items(
                    items = data,
                    key = { item -> item.hashCode() }
                ) { item ->
                    if (onTapItem != null) {
                        NeoClickable(
                            Modifier.padding(horizontal = 16.dp),
                            transitionType = tapTransition,
                            action = { onTapItem(item) }
                        ) {
                            itemContent(item)
                        }
                    } else {
                        Row(Modifier.padding(horizontal = 16.dp)) {
                            itemContent(item)
                        }
                    }
                }

                footerContent?.let {
                    item {
                        it()
                    }
                }
                if (isLoadingMore == true) {
                    item {
                        NeoLoadingIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                }

                if (hasMore && onLoadMore != null) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                        )
                        LaunchedEffect(Unit) {
                            onLoadMore()
                        }
                    }
                }
            }

            state is LoadingState.Idle || (state is LoadingState.Loaded) || (state is LoadingState.Refreshing) -> {
                item {
                    NeoPlaceholder(placeholderText ?: "아직 보여드릴 수 있는 내용이 없어요.")
                }
            }

            state is LoadingState.Loading -> {
                item {
                    NeoLoadingIndicator()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
