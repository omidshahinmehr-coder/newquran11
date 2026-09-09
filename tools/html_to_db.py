#!/usr/bin/env python3
"""
html_to_db.py
-------------
این اسکریپت را خودتان روی سیستم خودتان اجرا می‌کنید تا فایل‌های HTML که خودتان
دانلود کرده‌اید (ترجمه انصاریان و تفسیر البرهان) را به دیتابیس SQLite برنامه
تبدیل کند. من (Claude) خودم این کتاب‌ها را دانلود یا در پاسخ کپی نمی‌کنم؛
شما فایل‌های HTML را که خودتان دارید، در پوشه کنار این اسکریپت قرار می‌دهید.

پیش‌نیاز: pip install beautifulsoup4 lxml

نحوه استفاده:
    python html_to_db.py \
        --translation ansariyan.htm \
        --tafsir alborhan.htm \
        --out quran.db

نکته مهم: ساختار HTML سایت‌های مختلف با هم فرق دارد، بنابراین توابع
parse_translation() و parse_tafsir() زیر را باید بر اساس ساختار واقعی
فایل‌های خودتان تنظیم کنید. راهنمای هر تابع در کامنت‌های داخلش آمده.
اگر بخواهید، می‌توانید چند خط نمونه (نه کل کتاب) از ساختار HTML را برایم
بفرستید تا selectorهای دقیق را برایتان بنویسم.
"""

import argparse
import re
import sqlite3
from pathlib import Path

from bs4 import BeautifulSoup


def parse_translation(html_path: Path):
    """
    ساختار دقیق فایل انصاریان (ghbook.ir) که بررسی شد:
        <H2 class=content_h2>نام سوره</H2>
        <H3 class=content_h3>ترجمه آیه N</H3>
        <P class=content_paragraph><SPAN class=content_text>متن ترجمه</SPAN></P>

    خروجی: لیستی از دیکشنری‌های {surah_order, surah_name, ayah, text}
    surah_order = شماره ترتیبی سوره بر اساس ترتیب ظاهرشدن در فایل (1..114)
    که چون فایل به ترتیب استاندارد قرآن است، همان surahNumber واقعی است.
    """
    soup = BeautifulSoup(html_path.read_text(encoding="utf-8-sig", errors="ignore"), "lxml")

    results = []
    surah_order = 0
    current_surah_name = None
    ayah_num_re = re.compile(r"ترجمه\s+آیه\s+(\d+)")

    # عناصر مرتبط را به ترتیب سند پیمایش می‌کنیم
    for tag in soup.find_all(["h2", "h3"]):
        if tag.name == "h2" and "content_h2" in (tag.get("class") or []):
            title = tag.get_text(strip=True)
            if title == "مشخصات کتاب":
                continue
            surah_order += 1
            current_surah_name = title
        elif tag.name == "h3" and "content_h3" in (tag.get("class") or []):
            m = ayah_num_re.search(tag.get_text(strip=True))
            if not m or current_surah_name is None:
                continue
            ayah_num = int(m.group(1))
            # پاراگراف متن، خواهر بعدیِ H3 در همان DIV است
            p = tag.find_next_sibling("p", class_="content_paragraph")
            if p is None:
                continue
            text = p.get_text(strip=True)
            results.append({
                "surah_order": surah_order,
                "surah_name": current_surah_name,
                "ayah": ayah_num,
                "text": text,
            })
    return results


def parse_tafsir(html_path: Path):
    """
    ساختار دقیق فایل البرهان (ghbook.ir) که بررسی شد:
        تیترهای h3/h4 به شکل زیر، شماره سوره و بازه آیات را مستقیم دارند:
            سورة الفاتحة(1): آية 1 ..... ص : 95
            سورة البقرة(2): الآيات 11 الي 12 ..... ص : 123
        متن تفسیر در <P class=content_paragraph> بعد از این تیتر می‌آید،
        تا رسیدن به تیتر بازه‌ی بعدی. زیرتیترهای h5 داخل یک بازه (مثلا
        "ثواب فاتحة الكتاب...") بخشی از همان بازه محسوب می‌شوند.
        تیترهای h3/h4 که با این الگو مطابقت ندارند (مقدمه، فصل‌های کتاب)
        باعث بستن بافر جاری می‌شوند (متن آن‌ها به هیچ آیه‌ای وصل نمی‌شود).

    خروجی: لیستی از {surah_number, surah_name, start_ayah, end_ayah, text}
    """
    soup = BeautifulSoup(html_path.read_text(encoding="utf-8-sig", errors="ignore"), "lxml")
    range_re = re.compile(r"سورة\s+(.+?)\s*\((\d+)\)[^:]*:\s*(الآيات|آية)\s*(.+?)\s*\.{2,}")

    results = []
    current = None
    buffer = []

    def flush():
        if current and buffer:
            results.append({
                "surah_number": current["surah_num"],
                "surah_name": current["surah_name"],
                "start_ayah": current["start_ayah"],
                "end_ayah": current["end_ayah"],
                "text": "\n".join(buffer).strip(),
            })

    for tag in soup.find_all(["h3", "h4", "h5", "p"]):
        if tag.name in ("h3", "h4", "h5"):
            txt = tag.get_text(strip=True)
            m = range_re.search(txt)
            if m:
                flush()
                name = m.group(1).strip()
                surah_num = int(m.group(2))
                nums = re.findall(r"\d+", m.group(4))
                if len(nums) >= 2:
                    start, end = int(nums[0]), int(nums[1])
                else:
                    start = end = int(nums[0])
                current = {"surah_num": surah_num, "surah_name": name,
                           "start_ayah": start, "end_ayah": end}
                buffer = []
            elif tag.name in ("h3", "h4"):
                # تیتر سطح بالا و غیرمرتبط (عنوان سوره، مقدمه، فصل دیگر کتاب) -> بستن بافر جاری
                flush()
                current = None
                buffer = []
            # h5 غیرمرتبط (مثل "اشارة"، "فضلها") -> زیرتیتر داخل همان بازه است، بافر حفظ می‌شود
        elif tag.name == "p" and "content_paragraph" in (tag.get("class") or []):
            if current is not None:
                buffer.append(tag.get_text(strip=True))
    flush()
    return results


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--translation", type=Path, required=True)
    ap.add_argument("--tafsir", type=Path, required=False, default=None)
    ap.add_argument("--out", type=Path, default=Path("quran.db"))
    args = ap.parse_args()

    conn = sqlite3.connect(args.out)
    cur = conn.cursor()

    cur.executescript(
        """
        CREATE TABLE IF NOT EXISTS ayah (
            globalAyahId INTEGER PRIMARY KEY,
            surahNumber INTEGER,
            surahNameFa TEXT,
            ayahNumber INTEGER,
            textArabic TEXT
        );
        CREATE VIRTUAL TABLE IF NOT EXISTS ayah_fts USING fts4(
            content='ayah', textArabic
        );
        CREATE TABLE IF NOT EXISTS translation (
            globalAyahId INTEGER PRIMARY KEY,
            translator TEXT,
            textFa TEXT
        );
        CREATE VIRTUAL TABLE IF NOT EXISTS translation_fts USING fts4(
            content='translation', textFa
        );
        CREATE TABLE IF NOT EXISTS tafsir (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source TEXT,
            surahNumber INTEGER,
            startAyahId INTEGER,
            endAyahId INTEGER,
            textFa TEXT
        );
        CREATE VIRTUAL TABLE IF NOT EXISTS tafsir_fts USING fts4(
            content='tafsir', textFa
        );
        """
    )

    # NOTE: جدول ayah (متن عربی) را این اسکریپت پر نمی‌کند چون منبع جداگانه‌ای
    # برای متن عربی قرآن لازم دارید (مثلاً tanzil.net که متن عربی را با مجوز
    # آزاد منتشر کرده). آن فایل را دانلود و مشابه همین الگو import کنید.

    translations = parse_translation(args.translation)

    # globalAyahId بر اساس ترتیب واقعی ظاهرشدن در فایل محاسبه می‌شود
    # (چون فایل به ترتیب استاندارد قرآن از سوره 1 تا 114 است)
    gid_map = {}  # (surah_order, ayah) -> globalAyahId
    gid = 0
    for row in translations:
        gid += 1
        gid_map[(row["surah_order"], row["ayah"])] = gid

    for row in translations:
        g = gid_map[(row["surah_order"], row["ayah"])]
        # ردیف پایه در جدول ayah (متن عربی بعداً از منبع دیگر پر می‌شود)
        cur.execute(
            "INSERT OR IGNORE INTO ayah (globalAyahId, surahNumber, surahNameFa, ayahNumber, textArabic) "
            "VALUES (?,?,?,?,?)",
            (g, row["surah_order"], row["surah_name"], row["ayah"], ""),
        )
        cur.execute(
            "INSERT OR REPLACE INTO translation (globalAyahId, translator, textFa) VALUES (?,?,?)",
            (g, "انصاریان", row["text"]),
        )

    tafsirs = parse_tafsir(args.tafsir) if args.tafsir else []
    skipped = 0
    for row in tafsirs:
        start_gid = gid_map.get((row["surah_number"], row["start_ayah"]))
        end_gid = gid_map.get((row["surah_number"], row["end_ayah"])) or start_gid
        if start_gid is None:
            skipped += 1
            continue
        cur.execute(
            "INSERT INTO tafsir (source, surahNumber, startAyahId, endAyahId, textFa) VALUES (?,?,?,?,?)",
            ("البرهان فی تفسیر القرآن - سید هاشم بحرانی", row["surah_number"], start_gid, end_gid, row["text"]),
        )
    if skipped:
        print(f"Warning: skipped {skipped} tafsir blocks with unresolved ayah references")

    conn.commit()
    conn.close()
    print(f"Done. Wrote {len(translations)} translation rows and {len(tafsirs)} tafsir rows to {args.out}")


if __name__ == "__main__":
    main()
