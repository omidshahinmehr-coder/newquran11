package com.lbo.quran.audio

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** نگه‌دارنده‌ی مشترک نمونه‌ی ExoPlayer بین AudioPlaybackController (که دستورهای پخش را صادر
 *  می‌کند) و QuranAudioService (که همان پخش‌کننده را به یک MediaSession پس‌زمینه‌دار وصل می‌کند
 *  تا نوتیفیکیشن/کنترل هدفون و پخش هنگام بسته‌بودن صفحه هم کار کند). AudioPlaybackController
 *  باید پیش از استارت سرویس، پخش‌کننده را اینجا مقداردهی کرده باشد. */
object QuranAudioPlayerHolder {
    var player: ExoPlayer? = null
}

class QuranAudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = QuranAudioPlayerHolder.player ?: ExoPlayer.Builder(this).build().also {
            QuranAudioPlayerHolder.player = it
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        // خودِ پلیر عمداً release نمی‌شود چون ممکن است AudioPlaybackController هنوز به آن ارجاع
        // داشته باشد (مثلاً سرویس توسط سیستم موقتاً متوقف شده). با فراخوانی بعدی پخش، سرویس و
        // MediaSession دوباره روی همان پلیر ساخته می‌شوند.
        super.onDestroy()
    }
}
