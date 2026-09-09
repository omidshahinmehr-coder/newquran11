# قرآن کریم — اپ اندروید (اسکلت پروژه)

جستجوی یکپارچه در متن قرآن، ترجمه انصاریان و تفسیر البرهان (سید هاشم بحرانی)،
با استفاده از Kotlin + Jetpack Compose + SQLite خام (FTS4).

## ساختار
```
app/
  src/main/java/com/lbo/quran/
    data/    Entities.kt, QuranDao.kt, AppDatabase.kt, QuranRepository.kt
    ui/      QuranViewModel.kt, SearchScreen.kt, SurahScreen.kt
    MainActivity.kt
tools/
  html_to_db.py   ← اسکریپت تبدیل فایل‌های HTML شما به دیتابیس quran.db
```

## مراحل راه‌اندازی

1. **متن عربی قرآن**: یک منبع متن عربی با مجوز آزاد (مثلاً tanzil.net) دانلود
   کنید و جدول `ayah` را در `quran.db` پر کنید (اسکریپت فعلی فقط ترجمه و تفسیر
   را پر می‌کند، چون این دو را شما تهیه می‌کنید).

2. **ترجمه و تفسیر**: فایل‌های HTML که خودتان از لینک‌های ghbook.ir دانلود
   کرده‌اید را کنار `tools/html_to_db.py` بگذارید.

3. توابع `parse_translation()` و `parse_tafsir()` در `html_to_db.py` را باز کنید
   و ساختار HTML واقعی فایل‌های خودتان را بررسی کنید (بازکردن با مرورگر و View
   Source). این توابع فعلاً یک الگوی عمومی دارند (تشخیص عنوان سوره با کلمه‌ی
   "سوره" و شماره آیه داخل پرانتز/کروشه) — اگر ساختار فایل شما فرق دارد
   selectorها را با BeautifulSoup اصلاح کنید.

4. جدول `SURAH_META` در همان فایل را با نام و تعداد آیات هر ۱۱۴ سوره کامل کنید
   (این فقط داده‌ی ساختاری عمومی است، نه متن کتاب).

5. اجرا کنید:
   ```
   pip install beautifulsoup4 lxml
   python tools/html_to_db.py --translation ansariyan.htm --tafsir alborhan.htm --out quran.db
   ```

6. فایل خروجی `quran.db` را در `app/src/main/assets/quran.db` کپی کنید
   (پوشه `assets` را اگر وجود ندارد بسازید).

7. پروژه را در Android Studio باز کنید (Open → پوشه QuranApp) و Run بزنید.

## نکات
- جستجو از FTS4 استفاده می‌کند؛ برای عبارات چندکلمه‌ای به‌صورت AND ترکیب می‌شود.
- می‌توانید بعداً بوکمارک، حالت شب/روز، پخش صوت تلاوت و... اضافه کنید.
- اگر ساختار HTML فایل‌های خودتان را نمی‌دانید چطور parse کنید، چند خط از
  تگ‌های HTML اطراف یک آیه نمونه را برایم بفرستید تا selector دقیق بنویسم.
