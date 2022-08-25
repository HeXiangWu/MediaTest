package com.example.mediasessiontest.viewmodel

import android.app.Application
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.Spanned
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.mediasessiontest.R
import java.lang.ref.SoftReference

/**
 * 作用:
 */
class MusicViewModel(private var mApplication: Application?) : BaseViewModel(mApplication) {
    private var playbackInfo: Spanned? = null
    private val TAG = "MusicViewModel"
    //设置初始drawable文件源id
    private var playbackDrawable = 0
    private var record: SoftReference<LayerDrawable?>? = null

    @RequiresApi(Build.VERSION_CODES.N)
    fun SyncMusicInformation() {
        if (mMediaControllerCompat == null) {
            Log.e(TAG, "SyncMusicInformation: controller为空")
            return
        }
        val lastMetadata = mMediaControllerCompat!!.metadata ?: return
        //歌名-歌手
        val title = lastMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artist = lastMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        //Log.d(TAG, "onChildrenLoaded: "+lastMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
    }

    fun setPlaybackState(@PlaybackStateCompat.State playState: Int) {
        val state = playState == PlaybackStateCompat.STATE_PLAYING || playState == PlaybackStateCompat.STATE_PAUSED || playState == PlaybackStateCompat.STATE_STOPPED || playState == PlaybackStateCompat.STATE_NONE
        if (!state) {
            return
        }
        //Log.e(TAG, "setPlaybackState: "+playState);
        playbackDrawable = if (playState == PlaybackStateCompat.STATE_PLAYING) R.drawable.iv_main_pause else R.drawable.iv_main_play
    }
}