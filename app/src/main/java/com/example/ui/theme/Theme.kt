package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AmoaColors(
    val pageBg: Color,
    val panelBg: Color,
    val keyBg: Color,
    val keyBorder: Color,
    val keyText: Color,
    val opBg: Color,
    val opBorder: Color,
    val opText: Color,
    val funcBg: Color,
    val funcBorder: Color,
    val funcText: Color,
    val equalsBg: Color,
    val equalsText: Color,
    val brass: Color,
    val teal: Color,
    val text: Color,
    val textDim: Color,
    val border: Color,
    val paperBg: Color,
    val paperLine: Color,
    val isDark: Boolean
)

val LocalAmoaColors = staticCompositionLocalOf {
    AmoaColors(
        pageBg = DarkPageBg,
        panelBg = DarkPanelBg,
        keyBg = DarkKeyBg,
        keyBorder = DarkKeyBorder,
        keyText = DarkText,
        opBg = DarkOpBg,
        opBorder = DarkOpBorder,
        opText = AmoaGold,
        funcBg = DarkFuncBg,
        funcBorder = DarkFuncBorder,
        funcText = AmoaTeal,
        equalsBg = AmoaGold,
        equalsText = Color(0xFF1A1408),
        brass = AmoaGold,
        teal = AmoaTeal,
        text = DarkText,
        textDim = DarkTextDim,
        border = DarkBorder,
        paperBg = Color(0xFF1F2530),
        paperLine = Color(0xFF2C3342),
        isDark = true
    )
}

private val DarkAmoaColors = AmoaColors(
    pageBg = DarkPageBg,
    panelBg = DarkPanelBg,
    keyBg = DarkKeyBg,
    keyBorder = DarkKeyBorder,
    keyText = DarkText,
    opBg = DarkOpBg,
    opBorder = DarkOpBorder,
    opText = AmoaGold,
    funcBg = DarkFuncBg,
    funcBorder = DarkFuncBorder,
    funcText = AmoaTeal,
    equalsBg = AmoaGold,
    equalsText = Color(0xFF1A1408),
    brass = AmoaGold,
    teal = AmoaTeal,
    text = DarkText,
    textDim = DarkTextDim,
    border = DarkBorder,
    paperBg = Color(0xFF1C222C),
    paperLine = Color(0xFF2E3846),
    isDark = true
)

private val LightAmoaColors = AmoaColors(
    pageBg = LightPageBg,
    panelBg = LightPanelBg,
    keyBg = LightKeyBg,
    keyBorder = LightKeyBorder,
    keyText = LightText,
    opBg = LightOpBg,
    opBorder = LightOpBorder,
    opText = AmoaGoldLight,
    funcBg = LightFuncBg,
    funcBorder = LightFuncBorder,
    funcText = AmoaTealLight,
    equalsBg = Color(0xFFF59E0B),
    equalsText = Color(0xFF1A1408),
    brass = AmoaGoldLight,
    teal = AmoaTealLight,
    text = LightText,
    textDim = LightTextDim,
    border = LightBorder,
    paperBg = PaperBg,
    paperLine = PaperLine,
    isDark = false
)

private val M3DarkColorScheme = darkColorScheme(
    primary = AmoaGold,
    onPrimary = Color(0xFF1A1408),
    secondary = AmoaTeal,
    background = DarkPageBg,
    surface = DarkPanelBg,
    onBackground = DarkText,
    onSurface = DarkText
)

private val M3LightColorScheme = lightColorScheme(
    primary = AmoaGoldLight,
    onPrimary = Color(0xFF1A1408),
    secondary = AmoaTealLight,
    background = LightPageBg,
    surface = LightPanelBg,
    onBackground = LightText,
    onSurface = LightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val amoaColors = if (darkTheme) DarkAmoaColors else LightAmoaColors
    val m3Colors = if (darkTheme) M3DarkColorScheme else M3LightColorScheme

    CompositionLocalProvider(LocalAmoaColors provides amoaColors) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = Typography,
            content = content
        )
    }
}
