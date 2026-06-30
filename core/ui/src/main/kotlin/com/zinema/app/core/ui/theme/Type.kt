package com.zinema.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** App typography (blueprint §10.2). Unset roles fall back to Material defaults. */
val ZinemaTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color.White),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
