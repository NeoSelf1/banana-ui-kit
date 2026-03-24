package com.neon.core.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BananaTheme(
    typography: NeoTypography = NeoTypography(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNeoTypography provides typography,
        LocalOverscrollConfiguration provides null
    ) {
        content()
    }
}

object BananaDesign {
    val typography: NeoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNeoTypography.current
}
