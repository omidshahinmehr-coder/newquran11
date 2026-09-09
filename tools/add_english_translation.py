#!/usr/bin/env python3
"""
add_english_translation.py
---------------------------
این اسکریپت را خودتان (روی سیستمی با اتصال اینترنت) اجرا می‌کنید تا ترجمه‌ی
انگلیسی «پیکتال» (Marmaduke Pickthall, «The Meaning of the Glorious Quran»،
اولین‌بار منتشرشده در ۱۹۳۰) را به quran.db اضافه کند.

چرا پیکتال؟ چون مترجم در سال ۱۹۳۶ درگذشته و این ترجمه سال‌هاست از حق
کپی‌رایت خارج شده (public domain) و آزادانه برای چنین کاربردهایی توزیع
می‌شود؛ به همین دلیل برخلاف ترجمه‌های معاصر (مثل Sahih International که
هنوز کپی‌رایت دارد) مشکلی برای استفاده در برنامه ندارد.

منبع داده: alquran.cloud API (نسخه‌ی en.pickthall) که فیلد "number" هر آیه
دقیقاً همان شماره‌ی پیوسته‌ی ۱ تا ۶۲۳۶ استاندارد است -- یعنی همان globalAyahId
که در quran.db استفاده شده، بدون نیاز به تبدیل یا نگاشت اضافه.

نحوه استفاده:
    pip install requests
    python add_english_translation.py --db quran.db
"""

import argparse
import sqlite3
from pathlib import Path

import requests

API_URL = "https://api.alquran.cloud/v1/quran/en.pickthall"
TRANSLATOR_NAME = "Pickthall"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", type=Path, default=Path("quran.db"))
    args = ap.parse_args()

    print("در حال دانلود ترجمه انگلیسی پیکتال از", API_URL)
    resp = requests.get(API_URL, timeout=60)
    resp.raise_for_status()
    data = resp.json()["data"]["surahs"]

    conn = sqlite3.connect(args.db)
    cur = conn.cursor()

    # اگر قبلاً این مترجم اضافه شده، ابتدا ردیف‌های قدیمی حذف شوند (اجرای دوباره امن باشد)
    cur.execute("DELETE FROM translation WHERE translator = ?", (TRANSLATOR_NAME,))

    inserted = 0
    for surah in data:
        for ayah in surah["ayahs"]:
            global_ayah_id = ayah["number"]  # همان globalAyahId استاندارد ۱..۶۲۳۶
            text = ayah["text"]
            cur.execute(
                "INSERT INTO translation (globalAyahId, translator, textFa) VALUES (?,?,?)",
                (global_ayah_id, TRANSLATOR_NAME, text),
            )
            inserted += 1

    conn.commit()
    conn.close()
    print(f"Done. Inserted {inserted} English (Pickthall) translation rows into {args.db}")
    if inserted != 6236:
        print(f"Warning: expected 6236 rows, got {inserted} -- please double-check the source.")


if __name__ == "__main__":
    main()
