package com.lbo.quran.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.lbo.quran.R

val NeiriziFont = FontFamily(Font(R.font.neirizi))
val EstedadFont = FontFamily(Font(R.font.estedad))
val SGKaraFont = FontFamily(Font(R.font.sgkara))
val Quran1Font = FontFamily(Font(R.font.quran1))
val BadrFont = FontFamily(Font(R.font.badr))
val MoshafFont = FontFamily(Font(R.font.moshaf, weight = FontWeight.Bold, style = FontStyle.Normal ))

// وزن و سبک فونت طاها به‌صورت صریح Normal تنظیم شده تا سیستم روی آن سنتز
// (شبه‌بولد/شبه‌ایتالیک) اعمال نکند؛ این سنتز باعث می‌شد نقطه‌های حروف
// (مثل ت و ی) نازک‌تر/کم‌رنگ‌تر از باقی حروف دیده شوند.
val TahaFont = FontFamily(Font(R.font.taha, weight = FontWeight.Bold, style = FontStyle.Normal))
//val TahaFont = FontFamily(Font(R.font.taha))
val Moshaf2Font = FontFamily(Font(R.font.moshaf2))
val Quran2Font = FontFamily(Font(R.font.quran2))
val VazirFont = FontFamily(Font(R.font.vazir))

data class FontOption(val key: String, val label: String, val family: FontFamily?)

/** فهرست یکسان همه فونت‌های موجود در برنامه؛ هم برای متن قرآن و هم برای ترجمه/تفسیر قابل انتخاب است */
val AllFontOptions = listOf(
    FontOption("taha", "طاها", TahaFont),
    FontOption("neirizi", "نیریزی", NeiriziFont),
    FontOption("sgkara", "اس‌جی‌کارا", SGKaraFont),
    FontOption("quran1", "قرآن ۱", Quran1Font),
    FontOption("quran2", "قرآن ۲", Quran2Font),
    FontOption("badr", "بدر", BadrFont),
    FontOption("moshaf", "مصحف", MoshafFont),
    FontOption("moshaf2", "مصحف ۲", Moshaf2Font),
    FontOption("estedad", "استعداد", EstedadFont),
    FontOption("vazir", "وزیر", VazirFont),
    FontOption("system", "پیش‌فرض سیستم", null)
)

// نام‌های قدیمی برای سازگاری با بقیه کد؛ هر دو از همان فهرست یکسان می‌خوانند
val QuranFontOptions = AllFontOptions
val TranslationFontOptions = AllFontOptions

fun quranFontByKey(key: String): FontFamily? =
    AllFontOptions.firstOrNull { it.key == key }?.family ?: TahaFont

fun translationFontByKey(key: String): FontFamily? =
    AllFontOptions.firstOrNull { it.key == key }?.family ?: EstedadFont

/** گزینه‌ی وزن فونت برای نمایش در تنظیمات؛ مقدار خام (raw) همان چیزی است که در
 *  AppSettings.quranFontWeight/translationFontWeight ذخیره می‌شود (-1 یعنی «پیش‌فرض فونت»). */
data class FontWeightOption(val raw: Int, val label: String)

val FontWeightOptions = listOf(
    FontWeightOption(-1, "پیش‌فرض فونت"),
    FontWeightOption(100, "نازک (Thin)"),
    FontWeightOption(200, "خیلی سبک (ExtraLight)"),
    FontWeightOption(300, "سبک (Light)"),
    FontWeightOption(400, "معمولی (Normal)"),
    FontWeightOption(500, "متوسط (Medium)"),
    FontWeightOption(600, "نیمه‌ضخیم (SemiBold)"),
    FontWeightOption(700, "ضخیم (Bold)"),
    FontWeightOption(800, "خیلی ضخیم (ExtraBold)"),
    FontWeightOption(900, "سیاه (Black)")
)

/** مقدار خام ذخیره‌شده در تنظیمات را به FontWeight کامپوز تبدیل می‌کند؛
 *  null یعنی «هیچ بازنویسی‌ای انجام نشود»، دقیقاً همان رفتار قبلی برنامه. */
fun resolveFontWeight(raw: Int): FontWeight? = if (raw <= 0) null else FontWeight(raw)

fun resolveFontStyle(italic: Boolean): FontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
