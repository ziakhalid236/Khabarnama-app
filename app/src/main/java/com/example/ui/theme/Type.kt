package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val playfairGoogleFont = GoogleFont("Playfair Display")
private val interGoogleFont = GoogleFont("Inter")
private val notoNastaliqGoogleFont = GoogleFont("Noto Nastaliq Urdu")
private val notoNaskhGoogleFont = GoogleFont("Noto Naskh Arabic")

// 1. English Refined Serif: Playfair Display with Regular/Medium/SemiBold/Bold/ExtraBold/Black + Serif system fallback
val PlayfairDisplay = FontFamily(
    Font(googleFont = playfairGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = playfairGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = playfairGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = playfairGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = playfairGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = playfairGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Black),
    Font(DeviceFontFamilyName("serif"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("serif"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("serif"), weight = FontWeight.SemiBold),
    Font(DeviceFontFamilyName("serif"), weight = FontWeight.Bold),
    Font(DeviceFontFamilyName("serif"), weight = FontWeight.ExtraBold),
    Font(DeviceFontFamilyName("serif"), weight = FontWeight.Black)
)

// 2. English Clean Sans: Inter with Regular/Medium/SemiBold/Bold + Sans-Serif system fallback
val Inter = FontFamily(
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.SemiBold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Bold)
)

// 3. Urdu Nastaliq: Noto Nastaliq Urdu with Regular/Medium/SemiBold/Bold + bundled full Noto Naskh Arabic fallback
val NotoNastaliqUrdu = FontFamily(
    Font(googleFont = notoNastaliqGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoNastaliqGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = notoNastaliqGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = notoNastaliqGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(resId = R.font.noto_naskh_arabic_regular, weight = FontWeight.Normal),
    Font(resId = R.font.noto_naskh_arabic_regular, weight = FontWeight.Medium),
    Font(resId = R.font.noto_naskh_arabic_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.noto_naskh_arabic_bold, weight = FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.SemiBold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Bold)
)

// 4. Urdu / Arabic Naskh: Noto Naskh Arabic with Regular/Medium/SemiBold/Bold + bundled full Noto Naskh Arabic fallback
val NotoNaskhArabic = FontFamily(
    Font(googleFont = notoNaskhGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoNaskhGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = notoNaskhGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = notoNaskhGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(resId = R.font.noto_naskh_arabic_regular, weight = FontWeight.Normal),
    Font(resId = R.font.noto_naskh_arabic_regular, weight = FontWeight.Medium),
    Font(resId = R.font.noto_naskh_arabic_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.noto_naskh_arabic_bold, weight = FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.SemiBold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)
