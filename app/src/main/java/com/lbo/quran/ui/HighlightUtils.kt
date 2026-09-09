package com.lbo.quran.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val HIGHLIGHT_STYLE = SpanStyle(background = Color(0xFFFFF176), color = Color.Black)

fun extractSearchTerms(rawQuery: String): List<String> =
    rawQuery.trim()
        .split(Regex("\\s+"))
        .map { it.replace(Regex("[\"'*]"), "") }
        .filter { it.isNotBlank() }

/** هایلایت ساده (بدون در نظر گرفتن اعراب) برای متن فارسی/انگلیسی ترجمه و تفسیر */
fun highlightPlain(text: String, terms: List<String>): AnnotatedString {
    val cleanTerms = terms.filter { it.isNotBlank() }
    if (cleanTerms.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val termsLower = cleanTerms.map { it.lowercase() }
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            var bestIdx = -1
            var bestLen = 0
            for (t in termsLower) {
                if (t.isEmpty()) continue
                val idx = lower.indexOf(t, i)
                if (idx in 0 until (if (bestIdx == -1) Int.MAX_VALUE else bestIdx) || (idx == bestIdx && t.length > bestLen)) {
                    if (bestIdx == -1 || idx < bestIdx) {
                        bestIdx = idx
                        bestLen = t.length
                    }
                }
            }
            if (bestIdx == -1) {
                append(text.substring(i))
                break
            }
            append(text.substring(i, bestIdx))
            withStyle(HIGHLIGHT_STYLE) { append(text.substring(bestIdx, bestIdx + bestLen)) }
            i = bestIdx + bestLen
        }
    }
}

private fun arabicCharClass(c: Char): String = when (c) {
    'ی', 'ي', 'ى' -> "[یيى]"
    'ک', 'ك' -> "[کك]"
    else -> Regex.escape(c.toString())
}

/** رجکسی که بین حروف عبارت جستجو، هر تعداد اعراب اختیاری را می‌پذیرد و حروف عربی/فارسی هم‌ارز را یکی می‌داند */
fun buildArabicTermRegex(term: String): Regex? {
    val cleaned = term.replace(Regex("\\p{M}"), "").replace("\u0640", "")
    if (cleaned.isEmpty()) return null
    val pattern = cleaned.map { arabicCharClass(it) }.joinToString("\\p{M}*")
    return try {
        Regex(pattern)
    } catch (e: Exception) {
        null
    }
}

/** هایلایت متن عربی قرآن؛ با متن اصلیِ اعراب‌دار کار می‌کند و محل تطبیق واقعی را پیدا می‌کند */
fun highlightArabic(text: String, terms: List<String>): AnnotatedString {
    val regexes = terms.mapNotNull { buildArabicTermRegex(it) }
    if (regexes.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            var bestStart = -1
            var bestEnd = -1
            for (r in regexes) {
                val m = r.find(text, i) ?: continue
                if (bestStart == -1 || m.range.first < bestStart) {
                    bestStart = m.range.first
                    bestEnd = m.range.last + 1
                }
            }
            if (bestStart == -1) {
                append(text.substring(i))
                break
            }
            append(text.substring(i, bestStart))
            withStyle(HIGHLIGHT_STYLE) { append(text.substring(bestStart, bestEnd)) }
            i = bestEnd
        }
    }
}

/** برای متن‌های طولانی (مثل تفسیر)، یک بازه‌ی محدود دور اولین محل تطبیق برمی‌گرداند */
fun smartTruncateAroundMatch(text: String, terms: List<String>, isArabic: Boolean, maxLen: Int = 220): String {
    if (text.length <= maxLen) return text
    val firstIdx = if (isArabic) {
        terms.mapNotNull { buildArabicTermRegex(it)?.find(text)?.range?.first }.minOrNull()
    } else {
        val lower = text.lowercase()
        terms.mapNotNull { val idx = lower.indexOf(it.lowercase()); if (idx >= 0) idx else null }.minOrNull()
    } ?: 0
    val start = maxOf(0, firstIdx - maxLen / 3)
    val end = minOf(text.length, start + maxLen)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < text.length) "…" else ""
    return prefix + text.substring(start, end) + suffix
}

/** آیا متن حاوی همه‌ی عبارات جستجوست؛ با تحمل اعراب و یکسان‌دانستن حروف عربی/فارسی هم‌ارز
 *  (ی/ي/ى و ک/ك). برای فیلتر کردن لیست‌ها (مثل صفحه‌ی مرور تفسیر) به‌جای contains() ساده به کار می‌رود،
 *  چون متن اصلی ذخیره‌شده در دیتابیس هنوز ترکیبی از این حروف و اعراب مختلف را دارد. */
fun matchesArabicTolerant(text: String, terms: List<String>): Boolean {
    val regexes = terms.mapNotNull { buildArabicTermRegex(it) }
    if (regexes.isEmpty()) return true
    return regexes.all { it.containsMatchIn(text) }
}
