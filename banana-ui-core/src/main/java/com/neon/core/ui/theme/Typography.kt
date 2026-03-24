package com.neon.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neon.core.ui.R

// iOS와 동일한 Pretendard 폰트 패밀리 정의
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

data class NeoTypography(
    // Display - 태블릿/대형 디스플레이용
    val display1: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 128.sp),
    val display2: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 80.sp),
    val display3: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 72.sp),
    val display4: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 48.sp),

    // Headline
    val headline1: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = 0.sp),
    val headline2: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 0.sp),
    val headline3: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.sp),
    val headline4: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.sp),
    val headline5: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.sp),
    val headline6: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 0.sp),

    // Subhead
    val subhead1: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.sp),
    val subhead2: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = 0.sp),
    val subhead3: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.15.sp),
    val subhead4: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 0.15.sp),
    val subhead5: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.1.sp),
    val subhead6: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.5.sp),

    // Body
    val body1: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.25.sp),
    val body2: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.25.sp),
    val body3: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.4.sp),
    val body4: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.4.sp),
    val body5: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.4.sp),
    val body6: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.4.sp),

    // Caption
    val caption1: TextStyle = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)

val LocalNeoTypography = staticCompositionLocalOf { NeoTypography() }
