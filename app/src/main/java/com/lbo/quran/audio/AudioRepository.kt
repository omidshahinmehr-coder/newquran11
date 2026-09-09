package com.lbo.quran.audio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** پیشرفت یک عملیات دانلود/استخراج (۰..۱)؛ message برای نمایش وضعیت به کاربر است */
sealed class AudioProgress {
    data class Downloading(val fraction: Float) : AudioProgress()
    data class Extracting(val fraction: Float) : AudioProgress()
    data class Done(val fileCount: Int) : AudioProgress()
    data class Error(val message: String) : AudioProgress()
}

class AudioRepository(private val context: Context) {

    /** پوشه‌ی اختصاصی برنامه که فایل‌های صوتی آیات (با نام ۶ رقمی aId.mp3) در آن قرار می‌گیرند */
    private val audioDir: File
        get() {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            return File(base, "audio").apply { mkdirs() }
        }

    fun audioFileFor(aId: String): File = File(audioDir, "$aId.mp3")

    fun hasAudio(aId: String): Boolean = audioFileFor(aId).exists()

    /** تعداد فایل‌های صوتی موجود در دستگاه (برای نمایش وضعیت در تنظیمات) */
    fun countAvailableAudio(): Int = audioDir.listFiles { f -> f.extension == "mp3" }?.size ?: 0

    fun clearAllAudio() {
        audioDir.listFiles()?.forEach { it.delete() }
    }

    /** دانلود فایل zip از یک URL و استخراج آن در پوشه‌ی صوت؛ پیشرفت را به‌صورت جریانی گزارش می‌دهد */
    fun downloadAndExtract(url: String): Flow<AudioProgress> = callbackFlow {
        val tempZip = File(context.cacheDir, "quran_audio_download.zip")
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 20_000
                connect()
            }
            if (connection.responseCode !in 200..299) {
                trySend(AudioProgress.Error("سرور خطا برگرداند: کد ${connection.responseCode}"))
                close()
                return@callbackFlow
            }
            val totalBytes = connection.contentLength.takeIf { it > 0 } ?: -1
            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempZip).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val fraction = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                        trySend(AudioProgress.Downloading(fraction.coerceIn(0f, 1f)))
                    }
                }
            }

            val count = extractZipInternal(tempZip.inputStream()) { fraction ->
                trySend(AudioProgress.Extracting(fraction))
            }
            trySend(AudioProgress.Done(count))
        } catch (e: Exception) {
            trySend(AudioProgress.Error(e.message ?: "خطای ناشناخته در دانلود"))
        } finally {
            tempZip.delete()
            close()
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    /** استخراج یک فایل zip انتخاب‌شده از حافظه‌ی محلی (از طریق Storage Access Framework) */
    fun extractFromLocalUri(uri: Uri): Flow<AudioProgress> = callbackFlow {
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                trySend(AudioProgress.Error("امکان باز کردن فایل انتخاب‌شده وجود نداشت"))
                close()
                return@callbackFlow
            }
            val count = extractZipInternal(stream) { fraction ->
                trySend(AudioProgress.Extracting(fraction))
            }
            trySend(AudioProgress.Done(count))
        } catch (e: Exception) {
            trySend(AudioProgress.Error(e.message ?: "خطای ناشناخته در استخراج فایل"))
        } finally {
            close()
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    /** فایل zip را می‌خواند و فقط ورودی‌های .mp3 با نام معتبر ۶ رقمی را در پوشه‌ی صوت استخراج می‌کند.
     *  چون تعداد کل ورودی‌های zip از قبل معلوم نیست (ZipInputStream استریمی است)، درصد پیشرفت
     *  تقریبی و بر اساس تعداد فایل استخراج‌شده تا کنون (نسبت به ۶۲۳۶ آیه) محاسبه می‌شود. */
    private fun extractZipInternal(input: InputStream, onProgress: (Float) -> Unit): Int {
        var count = 0
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = File(entry.name).name // نادیده گرفتن مسیرهای پوشه‌ای داخل zip
                if (!entry.isDirectory && name.matches(Regex("\\d{6}\\.mp3"))) {
                    val outFile = File(audioDir, name)
                    FileOutputStream(outFile).use { out ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                        }
                    }
                    count++
                    onProgress((count.toFloat() / TOTAL_AYAH_COUNT).coerceIn(0f, 1f))
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return count
    }

    companion object {
        private const val TOTAL_AYAH_COUNT = 6236
    }
}
