package com.lbo.quran.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private fun TextStyle.withEstedad() = this.copy(fontFamily = EstedadFont)

val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.withEstedad(),
        displayMedium = base.displayMedium.withEstedad(),
        displaySmall = base.displaySmall.withEstedad(),
        headlineLarge = base.headlineLarge.withEstedad(),
        headlineMedium = base.headlineMedium.withEstedad(),
        headlineSmall = base.headlineSmall.withEstedad(),
        titleLarge = base.titleLarge.withEstedad(),
        titleMedium = base.titleMedium.withEstedad(),
        titleSmall = base.titleSmall.withEstedad(),
        bodyLarge = base.bodyLarge.withEstedad(),
        bodyMedium = base.bodyMedium.withEstedad(),
        bodySmall = base.bodySmall.withEstedad(),
        labelLarge = base.labelLarge.withEstedad(),
        labelMedium = base.labelMedium.withEstedad(),
        labelSmall = base.labelSmall.withEstedad(),
    )
}
