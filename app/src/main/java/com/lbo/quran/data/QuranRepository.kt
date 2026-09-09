package com.lbo.quran.data

import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val TR_LANG_FA = "fa"
const val TR_LANG_EN = "en"
const val TAFSIR_LANG_AR = "ar"
const val TAFSIR_LANG_FA = "fa"

class QuranRepository(private val context: Context) {

    private val db get() = QuranDatabaseHelper.getDatabase(context)

    suspend fun getAllWords(): List<WordEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<WordEntity>()
        val cursor = db.rawQuery(
            "SELECT a_id, w_text, w_type, w_meaningAR, w_meaningFA, w_composition FROM Words_taha ORDER BY w_id",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                result += WordEntity(
                    aId = it.getString(0),
                    text = it.getString(1) ?: "",
                    type = it.getInt(2),
                    meaningAr = it.getString(3) ?: "",
                    meaningFa = it.getString(4) ?: "",
                    composition = it.getString(5) ?: ""
                )
            }
        }
        result
    }

    // ---------- سوره‌ها ----------

    suspend fun getSurahList(): List<SurahInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SurahInfo>()
        val cursor = db.rawQuery(
            "SELECT s_id, s_nameFA, s_nameAR, s_nameEN, s_nameMeaning, s_comments, " +
                "s_tedadAye, s_firstPage, s_lastPage, s_location FROM Sure ORDER BY s_id",
            null
        )
        cursor.use {
            while (it.moveToNext()) result += it.toSurahInfo()
        }
        result
    }

    suspend fun getSurahName(surahNumber: Int): String = withContext(Dispatchers.IO) {
        val cursor = db.rawQuery("SELECT s_nameFA FROM Sure WHERE s_id = ?", arrayOf(surahNumber.toString()))
        cursor.use { if (it.moveToFirst()) it.getString(0) else "" }
    }

    // ---------- آیات ----------

    suspend fun getAllAyat(): List<AyahEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AyahEntity>()
        val cursor = db.rawQuery(
            "SELECT a_id, Sure, Aye, text, Page, Hizb, Joz FROM Quran_Ayat ORDER BY a_id",
            null
        )
        cursor.use {
            while (it.moveToNext()) result += it.toAyah()
        }
        result
    }

    suspend fun getSurahAyat(surahNumber: Int): List<AyahEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AyahEntity>()
        val cursor = db.rawQuery(
            "SELECT a_id, Sure, Aye, text, Page, Hizb, Joz FROM Quran_Ayat WHERE Sure = ? ORDER BY Aye",
            arrayOf(surahNumber.toString())
        )
        cursor.use {
            while (it.moveToNext()) result += it.toAyah()
        }
        result
    }

    suspend fun getAyahsByIds(ids: List<String>): List<AyahEntity> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val result = mutableListOf<AyahEntity>()
        val cursor = db.rawQuery(
            "SELECT a_id, Sure, Aye, text, Page, Hizb, Joz FROM Quran_Ayat WHERE a_id IN ($placeholders)",
            ids.toTypedArray()
        )
        cursor.use {
            while (it.moveToNext()) result += it.toAyah()
        }
        result
    }

    // ---------- جزء‌ها ----------

    suspend fun getJuzList(): List<JuzInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<JuzInfo>()
        val cursor = db.rawQuery(
            """
            SELECT a.a_juz, MIN(a.a_id) as startId
            FROM Aye a
            WHERE a.a_aye > 0
            GROUP BY a.a_juz
            ORDER BY a.a_juz
            """.trimIndent(),
            null
        )
        val juzStarts = mutableListOf<Pair<Int, String>>()
        cursor.use {
            while (it.moveToNext()) juzStarts += it.getInt(0) to it.getString(1)
        }
        for ((juzNum, startId) in juzStarts) {
            val surahNum = startId.substring(0, 3).toInt()
            val ayahNum = startId.substring(3, 6).toInt()
            val surahName = getSurahName(surahNum)
            result += JuzInfo(juzNum, startId, surahName, ayahNum)
        }
        result
    }

    // ---------- حزب‌ها ----------

    /** فهرست حزب‌ها (ستون Hizb جدول Quran_Ayat، از ۱ تا ۲۴۰) به همراه محل شروع هر کدام */
    suspend fun getHizbList(): List<HizbInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<HizbInfo>()
        val cursor = db.rawQuery(
            """
            SELECT Hizb, MIN(a_id) as startId
            FROM Quran_Ayat
            GROUP BY Hizb
            ORDER BY Hizb
            """.trimIndent(),
            null
        )
        val hizbStarts = mutableListOf<Pair<Int, String>>()
        cursor.use {
            while (it.moveToNext()) hizbStarts += it.getInt(0) to it.getString(1)
        }
        for ((hizbNum, startId) in hizbStarts) {
            val surahNum = startId.substring(0, 3).toInt()
            val ayahNum = startId.substring(3, 6).toInt()
            val surahName = getSurahName(surahNum)
            result += HizbInfo(hizbNum, startId, surahName, ayahNum)
        }
        result
    }

    // ---------- ترجمه ----------

    suspend fun getAllTranslations(language: String): Map<String, TranslationEntity> = withContext(Dispatchers.IO) {
        val table = if (language == TR_LANG_EN) "translation_en" else "translation_fa"
        val result = HashMap<String, TranslationEntity>()
        val cursor = db.rawQuery("SELECT a_id, tr_text FROM $table", null)
        cursor.use {
            while (it.moveToNext()) {
                val aId = it.getString(0)
                result[aId] = TranslationEntity(aId, language, it.getString(1))
            }
        }
        result
    }

    suspend fun getFootnotesForAyah(aId: String, language: String): List<TafsirEntity> = withContext(Dispatchers.IO) {
        val table = if (language == TAFSIR_LANG_FA) "tafsir_fa" else "tafsir_ar"
        val result = mutableListOf<TafsirEntity>()
        val cursor = db.rawQuery(
            "SELECT t_id, t_text, t_type, t_part, t_start, t_end, t_page FROM $table " +
                "WHERE t_type = 7 AND t_start != '000000' AND t_start <= ? " +
                "AND (CASE WHEN t_end IS NULL OR t_end = '' THEN t_start ELSE t_end END) >= ? " +
                "ORDER BY t_id",
            arrayOf(aId, aId)
        )
        cursor.use {
            while (it.moveToNext()) result += it.toTafsir(language)
        }
        result
    }

    // ---------- تفسیر ----------

    suspend fun getTafsirForAyah(aId: String, language: String): List<TafsirEntity> = withContext(Dispatchers.IO) {
        val table = if (language == TAFSIR_LANG_FA) "tafsir_fa" else "tafsir_ar"
        val result = mutableListOf<TafsirEntity>()
        val cursor = db.rawQuery(
            "SELECT t_id, t_text, t_type, t_part, t_start, t_end, t_page FROM $table " +
                "WHERE t_type = 0 AND t_start != '000000' AND t_start <= ? " +
                "AND (CASE WHEN t_end IS NULL OR t_end = '' THEN t_start ELSE t_end END) >= ? " +
                "ORDER BY t_id",
            arrayOf(aId, aId)
        )
        cursor.use {
            while (it.moveToNext()) result += it.toTafsir(language)
        }
        result
    }

    suspend fun getAllTafsir(language: String): List<TafsirEntity> = withContext(Dispatchers.IO) {
        val table = if (language == TAFSIR_LANG_FA) "tafsir_fa" else "tafsir_ar"
        val result = mutableListOf<TafsirEntity>()
        val cursor = db.rawQuery(
            "SELECT t_id, t_text, t_type, t_part, t_start, t_end, t_page FROM $table ORDER BY t_id",
            null
        )
        cursor.use {
            while (it.moveToNext()) result += it.toTafsir(language)
        }
        result
    }

    suspend fun getTafsirForSurah(surahNumber: Int, language: String): List<TafsirEntity> = withContext(Dispatchers.IO) {
        val table = if (language == TAFSIR_LANG_FA) "tafsir_fa" else "tafsir_ar"
        val prefix = String.format("%03d", surahNumber)
        val result = mutableListOf<TafsirEntity>()
        val cursor = db.rawQuery(
            "SELECT t_id, t_text, t_type, t_part, t_start, t_end, t_page FROM $table " +
                "WHERE t_start LIKE ? ORDER BY t_id",
            arrayOf("$prefix%")
        )
        cursor.use {
            while (it.moveToNext()) result += it.toTafsir(language)
        }
        result
    }

    /** یک پاراگراف تفسیر مشخص را با شناسه‌اش برمی‌گرداند (برای صفحه‌ی نشانک‌های من) */
    suspend fun getTafsirById(id: Long, language: String): TafsirEntity? = withContext(Dispatchers.IO) {
        val table = if (language == TAFSIR_LANG_FA) "tafsir_fa" else "tafsir_ar"
        val cursor = db.rawQuery(
            "SELECT t_id, t_text, t_type, t_part, t_start, t_end, t_page FROM $table WHERE t_id = ?",
            arrayOf(id.toString())
        )
        cursor.use { if (it.moveToFirst()) it.toTafsir(language) else null }
    }

    suspend fun getAyahIdsWithTafsir(): Set<String> = withContext(Dispatchers.IO) {
        val result = mutableSetOf<String>()
        for (table in listOf("tafsir_ar", "tafsir_fa")) {
            val cursor = db.rawQuery(
                "SELECT DISTINCT t_start, t_end FROM $table WHERE t_type = 0 AND t_start != '000000' AND t_start != ''",
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val start = it.getString(0) ?: continue
                    val end = it.getString(1)?.takeIf { e -> e.isNotBlank() } ?: start
                    if (start.length != 6 || end.length != 6) continue
                    val surah = start.substring(0, 3)
                    if (surah != end.substring(0, 3)) {
                        // بازه‌ی بین دو سوره‌ی متفاوت -- فقط دو سرِ بازه را علامت بزن (حالت نادر)
                        result += start
                        result += end
                        continue
                    }
                    val startAyah = start.substring(3, 6).toIntOrNull() ?: continue
                    val endAyah = end.substring(3, 6).toIntOrNull() ?: continue
                    for (a in startAyah..endAyah) {
                        result += surah + String.format("%03d", a)
                    }
                }
            }
        }
        result
    }

    // ---------- جستجو ----------

    suspend fun search(
        rawQuery: String,
        includeQuran: Boolean,
        includeTafsirAr: Boolean,
        includeTafsirFa: Boolean
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val ftsQuery = toFtsQuery(rawQuery)
        if (ftsQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()

        if (includeQuran) {
            val cursor = db.rawQuery(
                """
                SELECT q.a_id, q.Sure, s.s_nameFA, q.Aye, q.text
                FROM search_fts
                JOIN Search se ON se.rowid = search_fts.rowid
                JOIN Quran_Ayat q ON q.a_id = se.a_id
                JOIN Sure s ON s.s_id = q.Sure
                WHERE search_fts MATCH ?
                LIMIT 100
                """.trimIndent(),
                arrayOf(ftsQuery)
            )
            cursor.use {
                while (it.moveToNext()) results += it.toSearchResult("quran")
            }
        }

        if (includeTafsirAr) results += searchTafsir(ftsQuery, "tafsir_ar", "tafsir_ar_fts", "tafsir_ar")
        if (includeTafsirFa) results += searchTafsir(ftsQuery, "tafsir_fa", "tafsir_fa_fts", "tafsir_fa")

        results
    }

    private fun searchTafsir(ftsQuery: String, table: String, ftsTable: String, kind: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val cursor = db.rawQuery(
            """
            SELECT q.a_id, q.Sure, s.s_nameFA, q.Aye, tf.t_text, tf.t_id
            FROM $ftsTable
            JOIN $table tf ON tf.rowid = $ftsTable.rowid
            JOIN Quran_Ayat q ON q.a_id = tf.t_start
            JOIN Sure s ON s.s_id = q.Sure
            WHERE $ftsTable MATCH ? AND tf.t_start != '000000' AND tf.t_start != ''
            LIMIT 100
            """.trimIndent(),
            arrayOf(ftsQuery)
        )
        cursor.use {
            while (it.moveToNext()) results += it.toSearchResult(kind, tafsirIdColumn = 5)
        }
        return results
    }

    private fun Cursor.toAyah() = AyahEntity(
        aId = getString(0),
        surahNumber = getInt(1),
        ayahNumber = getInt(2),
        text = getString(3),
        page = getInt(4),
        hizb = getInt(5),
        juz = getInt(6)
    )

    private fun Cursor.toSurahInfo() = SurahInfo(
        surahNumber = getInt(0),
        nameFa = getString(1),
        nameAr = getString(2),
        nameEn = getString(3),
        nameMeaning = getString(4) ?: "",
        comments = getString(5) ?: "",
        ayahCount = getInt(6),
        firstPage = getInt(7),
        lastPage = getInt(8),
        isMakki = getInt(9) == 1
    )

    private fun Cursor.toTafsir(language: String) = TafsirEntity(
        id = getLong(0),
        text = getString(1) ?: "",
        type = getInt(2),
        part = getString(3) ?: "",
        startId = getString(4) ?: "",
        endId = getString(5) ?: "",
        page = getString(6) ?: "",
        language = language
    )

    private fun Cursor.toSearchResult(kind: String, tafsirIdColumn: Int? = null) = SearchResult(
        aId = getString(0),
        surahNumber = getInt(1),
        surahNameFa = getString(2),
        ayahNumber = getInt(3),
        snippet = getString(4) ?: "",
        kind = kind,
        tafsirId = tafsirIdColumn?.let { getLong(it) }
    )

    private fun toFtsQuery(raw: String): String {
        val normalized = raw.trim()
            .replace("ي", "ی").replace("ك", "ک")
            .replace(Regex("\\p{M}"), "")
            .replace("\u0640", "")
        if (normalized.isEmpty()) return ""
        val terms = normalized.split(Regex("\\s+"))
            .map { it.replace(Regex("[\"'*]"), "") }
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return ""
        return terms.joinToString(" ") { "$it*" }
    }
}
