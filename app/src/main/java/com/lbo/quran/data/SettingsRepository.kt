package com.lbo.quran.data

import android.content.Context

data class AppSettings(
    val quranFontKey: String = "taha",
    val quranFontSize: Float = 36f,
    val translationFontKey: String = "estedad",
    val translationFontSize: Float = 14f,
    val translationLanguage: String = "fa", // "fa" or "en"
    // پیش‌فرض «سپیا» (متن قهوه‌ای روی زمینه‌ی کرم) به‌جای سیاه/سفید خام انتخاب شده چون
    // برای مطالعه‌ی طولانی راحت‌تر است و خیرگی صفحه‌ی سفید را کم می‌کند.
    val quranTextColor: Int = 0xFF5B3A29.toInt(),
    val quranBackgroundColor: Int = 0xFFFBF3E0.toInt(),
    val tafsirTextColor: Int = 0xFF5B3A29.toInt(),
    val tafsirBackgroundColor: Int = 0xFFFBF3E0.toInt(),
    // وزن فونت به‌صورت عدد استاندارد ۱۰۰ تا ۹۰۰ (طبق FontWeight کامپوز)؛ مقدار -1 یعنی
    // «وزن پیش‌فرض همان فونت» (بدون بازنویسی)، دقیقاً همان رفتار قبلی برنامه.
    val quranFontWeight: Int = -1,
    val translationFontWeight: Int = -1,
    val quranFontItalic: Boolean = false,
    val translationFontItalic: Boolean = false,
    // فاصله‌ی حروف؛ برای متن قرآن به‌عمد پیش‌فرض صفر و توصیه‌شده است چون زیادکردنش
    // می‌تواند اتصال حروف عربی را بهم بریزد؛ برای ترجمه/تفسیر مشکلی ندارد.
    val quranLetterSpacing: Float = 0f,
    val translationLetterSpacing: Float = 0f,
    // ضریب فاصله‌ی خطوط (line-height)؛ مقادیر پیش‌فرض دقیقاً همان چیزی است که قبلاً
    // به‌صورت ثابت در کد بود (۱.۹ برابر برای قرآن، ۱.۶ برابر برای ترجمه/تفسیر).
    val quranLineHeightMultiplier: Float = 1.9f,
    val translationLineHeightMultiplier: Float = 1.6f,
    // آدرس دلخواه کاربر برای دانلود فایل zip صوت آیات
    val audioZipUrl: String = ""
)

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("quran_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        quranFontKey = prefs.getString(KEY_QURAN_FONT, "taha") ?: "taha",
        quranFontSize = prefs.getFloat(KEY_QURAN_SIZE, 36f),
        translationFontKey = prefs.getString(KEY_TR_FONT, "estedad") ?: "estedad",
        translationFontSize = prefs.getFloat(KEY_TR_SIZE, 14f),
        translationLanguage = prefs.getString(KEY_TR_LANG, "fa") ?: "fa",
        quranTextColor = prefs.getInt(KEY_QURAN_TEXT_COLOR, 0xFF5B3A29.toInt()),
        quranBackgroundColor = prefs.getInt(KEY_QURAN_BG_COLOR, 0xFFFBF3E0.toInt()),
        tafsirTextColor = prefs.getInt(KEY_TAFSIR_TEXT_COLOR, 0xFF5B3A29.toInt()),
        tafsirBackgroundColor = prefs.getInt(KEY_TAFSIR_BG_COLOR, 0xFFFBF3E0.toInt()),
        quranFontWeight = prefs.getInt(KEY_QURAN_FONT_WEIGHT, -1),
        translationFontWeight = prefs.getInt(KEY_TR_FONT_WEIGHT, -1),
        quranFontItalic = prefs.getBoolean(KEY_QURAN_ITALIC, false),
        translationFontItalic = prefs.getBoolean(KEY_TR_ITALIC, false),
        quranLetterSpacing = prefs.getFloat(KEY_QURAN_LETTER_SPACING, 0f),
        translationLetterSpacing = prefs.getFloat(KEY_TR_LETTER_SPACING, 0f),
        quranLineHeightMultiplier = prefs.getFloat(KEY_QURAN_LINE_HEIGHT, 1.9f),
        translationLineHeightMultiplier = prefs.getFloat(KEY_TR_LINE_HEIGHT, 1.6f),
        audioZipUrl = prefs.getString(KEY_AUDIO_URL, "") ?: ""
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_QURAN_FONT, settings.quranFontKey)
            .putFloat(KEY_QURAN_SIZE, settings.quranFontSize)
            .putString(KEY_TR_FONT, settings.translationFontKey)
            .putFloat(KEY_TR_SIZE, settings.translationFontSize)
            .putString(KEY_TR_LANG, settings.translationLanguage)
            .putInt(KEY_QURAN_TEXT_COLOR, settings.quranTextColor)
            .putInt(KEY_QURAN_BG_COLOR, settings.quranBackgroundColor)
            .putInt(KEY_TAFSIR_TEXT_COLOR, settings.tafsirTextColor)
            .putInt(KEY_TAFSIR_BG_COLOR, settings.tafsirBackgroundColor)
            .putInt(KEY_QURAN_FONT_WEIGHT, settings.quranFontWeight)
            .putInt(KEY_TR_FONT_WEIGHT, settings.translationFontWeight)
            .putBoolean(KEY_QURAN_ITALIC, settings.quranFontItalic)
            .putBoolean(KEY_TR_ITALIC, settings.translationFontItalic)
            .putFloat(KEY_QURAN_LETTER_SPACING, settings.quranLetterSpacing)
            .putFloat(KEY_TR_LETTER_SPACING, settings.translationLetterSpacing)
            .putFloat(KEY_QURAN_LINE_HEIGHT, settings.quranLineHeightMultiplier)
            .putFloat(KEY_TR_LINE_HEIGHT, settings.translationLineHeightMultiplier)
            .putString(KEY_AUDIO_URL, settings.audioZipUrl)
            .apply()
    }

    companion object {
        private const val KEY_QURAN_FONT = "quran_font_key"
        private const val KEY_QURAN_SIZE = "quran_font_size"
        private const val KEY_TR_FONT = "translation_font_key"
        private const val KEY_TR_SIZE = "translation_font_size"
        private const val KEY_TR_LANG = "translation_language"
        private const val KEY_QURAN_TEXT_COLOR = "quran_text_color"
        private const val KEY_QURAN_BG_COLOR = "quran_bg_color"
        private const val KEY_TAFSIR_TEXT_COLOR = "tafsir_text_color"
        private const val KEY_TAFSIR_BG_COLOR = "tafsir_bg_color"
        private const val KEY_QURAN_FONT_WEIGHT = "quran_font_weight"
        private const val KEY_TR_FONT_WEIGHT = "translation_font_weight"
        private const val KEY_QURAN_ITALIC = "quran_font_italic"
        private const val KEY_TR_ITALIC = "translation_font_italic"
        private const val KEY_QURAN_LETTER_SPACING = "quran_letter_spacing"
        private const val KEY_TR_LETTER_SPACING = "translation_letter_spacing"
        private const val KEY_QURAN_LINE_HEIGHT = "quran_line_height"
        private const val KEY_TR_LINE_HEIGHT = "translation_line_height"
        private const val KEY_AUDIO_URL = "audio_zip_url"
    }
}
