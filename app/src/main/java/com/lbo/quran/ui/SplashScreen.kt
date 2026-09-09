package com.lbo.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.EstedadFont
import kotlinx.coroutines.delay

private const val SALAWAT =
    "اَللّهُمَّ صَلِّ عَلَی مُحَمَّدِِ وَ آلِ مُحَمَّد\nوَ عَجِّل فَرَجَهُم وَ العَن اَعدائَهُم اَجمعین"

private val NightSkyBlue = Color(0xFF0A1128)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightSkyBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            SALAWAT,
            fontFamily = EstedadFont,
            color = Color.White,
            fontSize = 22.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
