package com.neon.core.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// iOS와 동일 - 다크모드/라이트모드 구분없이 같은 색상 사용
private val HumaniaColorScheme = lightColorScheme(
    // Primary 색상 - iOS primary50과 동일
    primary = Primary50,
    onPrimary = Color.White,
    primaryContainer = Primary10,
    onPrimaryContainer = Primary90,
    
    // Secondary 색상 - iOS secondary50과 동일
    secondary = Secondary50,
    onSecondary = Gray10,
    secondaryContainer = Secondary05,
    onSecondaryContainer = Secondary90,
    
    // Background & Surface - iOS 기본 색상들
    background = Gray10,
    onBackground = Gray90,
    surface = Gray10, // surfaceMedium
    onSurface = Color(0xFF1A1A1A), // gray90
    surfaceVariant = Color(0xFFF5F5F5), // surfaceDisabled
    onSurfaceVariant = Gray70,
    
    // Error 색상 - iOS warning과 동일
    error = Warning,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Outline - iOS outline과 동일
    outline = Color(0x1F000000), // 12% alpha black
    outlineVariant = Gray25
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HumaniaTheme(
    content: @Composable () -> Unit
) {
    // iOS와 동일하게 단일 팔레트를 사용하고, 시스템 바 제어는 Activity에서 처리한다.
    MaterialTheme(
        colorScheme = HumaniaColorScheme
    ){
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
            content = content
        )
    }
}
