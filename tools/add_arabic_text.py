#!/usr/bin/env python3
"""
add_arabic_text.py
-------------------
این اسکریپت را خودتان (روی سیستمی با اتصال اینترنت) اجرا می‌کنید تا متن عربی
قرآن را در quran.db (ستون خالی textArabic در جدول ayah) پر کند.

منبع متن عربی: پروژه quran-json (مشتق از متن عثمانی Tanzil.net)، منتشرشده با
مجوز Creative Commons که صراحتاً استفاده در اپلیکیشن را مجاز می‌داند، به شرط
ذکر منبع. لینک: https://github.com/risan/quran-json

نحوه استفاده:
    pip install requests
    python add_arabic_text.py --db quran.db
"""

import argparse
import json
import sqlite3
import urllib.request
from pathlib import Path

QURAN_JSON_URL = "https://cdn.jsdelivr.net/npm/quran-json@3.1.2/dist/quran.json"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", type=Path, default=Path("quran.db"))
    args = ap.parse_args()

    print("در حال دانلود متن عربی از", QURAN_JSON_URL)
    with urllib.request.urlopen(QURAN_JSON_URL) as resp:
        chapters = json.loads(resp.read().decode("utf-8"))

    conn = sqlite3.connect(args.db)
    cur = conn.cursor()

    updated = 0
    missing_surah_rows = 0
    gid = 0
    for chapter in chapters:
        surah_num = chapter["id"]
        for verse in chapter["verses"]:
            gid += 1
            cur.execute(
                "UPDATE ayah SET textArabic = ? WHERE globalAyahId = ?",
                (verse["text"], gid),
            )
            if cur.rowcount == 0:
                # ردیف پایه هنوز وجود ندارد (چون هنوز html_to_db.py را اجرا
                # نکرده‌اید) -> یک ردیف جدید با نام سوره عربی می‌سازیم
                cur.execute(
                    "INSERT INTO ayah (globalAyahId, surahNumber, surahNameFa, ayahNumber, textArabic) "
                    "VALUES (?,?,?,?,?)",
                    (gid, surah_num, chapter["name"], verse["id"], verse["text"]),
                )
                missing_surah_rows += 1
            updated += 1

    conn.commit()
    conn.close()
    print(f"Done. Updated/inserted {updated} ayat with Arabic text "
          f"({missing_surah_rows} were new rows).")


if __name__ == "__main__":
    main()
