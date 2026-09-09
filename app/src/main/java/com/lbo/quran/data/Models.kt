package com.lbo.quran.data

/** آیه با کلید یکتای a_id (شش رقمی: سه رقم شماره سوره + سه رقم شماره آیه) */
data class AyahEntity(
    val aId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String,
    val page: Int,
    val hizb: Int,
    val juz: Int
)

/**
 * یک کلمه/نشانه از جدول Words_taha.
 * w_type: 1=کلمه عادی، 3=شماره پایان آیه، 6=بخشی از بسم‌الله،
 *         0 و 4=علائم وقف (صلی/قلی/...)، 5=نشانه ربع‌حزب، 7=نشانه سجده
 */
data class WordEntity(
    val aId: String,
    val text: String,
    val type: Int,
    val meaningAr: String = "",
    val meaningFa: String = "",
    val composition: String = ""
)

data class SurahInfo(
    val surahNumber: Int,
    val nameFa: String,
    val nameAr: String,
    val nameEn: String,
    val nameMeaning: String,
    val comments: String,
    val ayahCount: Int,
    val firstPage: Int,
    val lastPage: Int,
    val isMakki: Boolean
)

data class TranslationEntity(
    val aId: String,
    val language: String, // "fa" یا "en"
    val text: String
)

data class TafsirEntity(
    val id: Long,
    val text: String,
    val type: Int,
    val part: String,
    val startId: String,
    val endId: String,
    val page: String,
    val language: String // "ar" یا "fa"
)

data class JuzInfo(
    val juzNumber: Int,
    val startAId: String,
    val startSurahName: String,
    val startAyahNumber: Int
)

/** یک واحد حزب (طبق ستون Hizb جدول Quran_Ayat؛ در این برنامه هر جزء به ۸ حزب تقسیم می‌شود) */
data class HizbInfo(
    val hizbNumber: Int,
    val startAId: String,
    val startSurahName: String,
    val startAyahNumber: Int
)

data class SearchResult(
    val aId: String,
    val surahNumber: Int,
    val surahNameFa: String,
    val ayahNumber: Int,
    val snippet: String,
    val kind: String, // "quran" | "tafsir_ar" | "tafsir_fa"
    val tafsirId: Long? = null // rowid ردیف تفسیر در جدول اصلی؛ برای پرش دقیق به همان پاراگراف
)

/** یک آیتم در فهرست پیوسته‌ی متن کامل قرآن (برای صفحه اصلی) */
sealed class ReadingItem {
    data class SurahHeader(val surahNumber: Int, val surahNameFa: String) : ReadingItem()
    data class Bismillah(val surahNumber: Int, val words: List<WordEntity>) : ReadingItem()
    data class Ayah(val ayah: AyahEntity, val surahNameFa: String, val words: List<WordEntity>) : ReadingItem()
}
