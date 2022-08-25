package com.example.mediasessiontest.service.manager

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import com.example.mediasessiontest.R
import java.io.IOException
import java.util.*

/**
 * 作用:
 */
class MediaPlayerManager(//
    private var mApplication: Application?,
    mediaSession: MediaSessionCompat?,
    notificationListener: NotificationListener?,
    focusChangeListener: AudioManager.OnAudioFocusChangeListener?) {
    private var mMediaSession: MediaSessionCompat?
    private var mMyAudioManager: MyAudioManager? = null
    private var mMediaPlayer: MediaPlayer?
    var isFirstPlay = true
        private set
    private var mCurrentPosition = -1
    private var mCurrentAudioLevel = 0

    //播放模式Flag
    private var DYQL_PLAYBACK_MODE = 0

    //MusicService 回调更新通知栏
    private var mNotificationListener: NotificationListener?

    interface NotificationListener {
        fun onUpdateNotification()
    }

    fun onDestroy() {
        if (mApplication != null) {
            mApplication = null
        }
        if (mMediaSession != null) {
            mMediaSession = null
        }
        if (mNotificationListener != null) {
            mNotificationListener = null
        }
        if (mMyAudioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMyAudioManager!!.onDestroy()
            }
            mMyAudioManager = null
        }
        releaseMediaPlayer()
    }

    fun releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            StopMediaPlayer()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    //MediaPlayer 相关方法
    fun StopMediaPlayer() {
        //Log.d(TAG, "StopMediaPlayer: ");
        if (isFirstPlay && mCurrentPosition <= 0) return
        if (mMediaPlayer != null) {
            if (isPlaying) {
                mMediaPlayer!!.pause()
                mMediaPlayer!!.seekTo(0)
                mMediaPlayer!!.stop()
            }
            //释放wifi锁 , 释放音频焦点
            if (mMyAudioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mMyAudioManager!!.releaseAudioFocus()
                }
            }
            //重置MediaPlayer
            mMediaPlayer!!.reset()
            //适用于上次播放有进度，但是第一次播放了这首歌曲，所以播放进度保留
            if (!isFirstPlay) resetCurrentPosition()
        } else println("MediaPlayer is null!")
    }

    fun setDataRes(path: String?) {
        StopMediaPlayer()
        try {
            mMediaPlayer!!.setDataSource(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun PlayFromUri() {
        if (mMediaPlayer == null || isPlaying) return
        if (!mMediaSession!!.isActive()) {
            mMediaSession!!.setActive(true)
        }
        if (mMyAudioManager != null) {
            volume = mCurrentAudioLevel
        }
        if (mCurrentPosition <= 0 || isFirstPlay) {
            mMediaSession?.setPlaybackState(
                newPlaybackState(PlaybackStateCompat.STATE_BUFFERING, null))
            mMediaPlayer!!.prepareAsync()
        } else { //暂停后继续播放
            mMediaPlayer!!.seekTo(mCurrentPosition)
            val bundle = Bundle()
            bundle.putBoolean("Continue_Playing_Tips", true)
            checkFocusPlay(bundle)
            Log.d(TAG, "PlayMediaPlayer: 暂停后播放")
        }
    }

    fun OnPause(notReleaseAudio: Boolean) {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying()) {
            //记录播放队列位置
            mCurrentPosition = mMediaPlayer!!.getCurrentPosition()
            mMediaPlayer!!.pause()
        }
        //停止播放音乐释放焦点
        if (!notReleaseAudio) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMyAudioManager!!.releaseAudioFocus()
            }
        }
        mMediaSession!!.setPlaybackState(newPlaybackState(PlaybackStateCompat.STATE_PAUSED, null))
        mNotificationListener!!.onUpdateNotification()
    }

    fun seekTo(pos: Long) {
        if (mMediaPlayer == null && mMediaSession == null) return
        val mMediaController: MediaControllerCompat = mMediaSession!!.getController()
        if (mMediaController.getPlaybackState().getState()
            == PlaybackStateCompat.STATE_PLAYING) mMediaPlayer!!.pause()
        mCurrentPosition = if (pos == 0L) 1 else pos.toInt()
        if (isFirstPlay) {
            val path: String = mMediaController.getMetadata()
                .getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            setDataRes(path)
        }
        PlayFromUri()
    }

    //***************************音量更改与获取****************************/
    fun checkAudioChange(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mCurrentAudioLevel != mMyAudioManager!!.volume
        } else {
            TODO("VERSION.SDK_INT < N")
        }
    }

    fun lowerTheVolume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMyAudioManager!!.lowerTheVolume()
        }
    }

    //设置播放流音量，防止静音时播放后，再调整音量无效的问题
    //Log.d(TAG, "getVolume: "+mMyAudioManager.getVolume());
    var volume: Int
        get() =//Log.d(TAG, "getVolume: "+mMyAudioManager.getVolume());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMyAudioManager!!.volume
            } else {
                TODO("VERSION.SDK_INT < N")
            }
        set(volume) {
            if (mMyAudioManager!!.setVolume(volume)) {
                //设置播放流音量，防止静音时播放后，再调整音量无效的问题
                val percent = String.format(Locale.CHINA, "%.2f",
                    volume.toFloat() / mMyAudioManager!!.maxVolume.toFloat()).toFloat()
                Log.d(TAG, "setVolume: $percent")
                if (mMediaPlayer != null) mMediaPlayer!!.setVolume(percent, percent)
                mCurrentAudioLevel = volume
            }
        }

    //Log.d(TAG, "getMaxVolume: "+mMyAudioManager.getMaxVolume());
    val maxVolume: Int
        get() =//Log.d(TAG, "getMaxVolume: "+mMyAudioManager.getMaxVolume());
            mMyAudioManager!!.maxVolume

    //***************************获取与更改播放进度****************************/
    fun resetCurrentPosition() {
        mCurrentPosition = 0
    }

    val currentPosition: Int
        get() = if (isPlaying) mMediaPlayer?.getCurrentPosition()!! else mCurrentPosition

    fun initCurrentPosition(position: Int) {
        mCurrentPosition = position
    }

    //***************************一些播放状态****************************/
    val isPlaying: Boolean
        get() = mMediaPlayer != null && mMediaPlayer!!.isPlaying()

    fun setLooping() {
        mMediaPlayer!!.setLooping(playbackMode == dyqlPlaybackModeRepeat)
    }

    //***************************MediaPlayer回调区****************************/
    private inner class onCompleteListener : MediaPlayer.OnCompletionListener {
        override fun onCompletion(mp: MediaPlayer) {
            Log.d(TAG, "onCompletion: ")
            //播放下一曲
            mMediaSession!!.getController().getTransportControls().skipToNext()
        }
    }

    private inner class onErrorListener : MediaPlayer.OnErrorListener {
        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            //当播放错误时，MediaPlayer执行此回调并停止播放，故isPlayerPrepared 状态应为true，已准备好播放
            /*if (!isPlayerPrepared) isPlayerPrepared = true;
            isError_Flag = true;//必须要设置不同Path才能继续播放*/
            if (what != -38 && extra != 0) mApplication!!.sendBroadcast(Intent("error"))
            Log.d(TAG, "onErrorListener: what:$what , extra = $extra")
            when (what) {
                1 -> if (extra == 1) Log.e(TAG, "播放错误，歌曲地址为空,请播放其他歌曲") else if (extra == 2) Log.e(TAG, "该音乐文件已损坏,请播放其他歌曲") else if (extra == 28) Log.e(TAG, "该音乐媒体对象为空，请重新打开App") else if (extra == -2147483648) Log.e(TAG, "音乐文件解码失败,请删除该文件,尝试播放网络版本") else Log.e(TAG, "播放错误,请播放其他歌曲")
                2 -> {
                    if (extra == 2) Log.d(TAG, "onError: 音乐文件解码失败,请播放其他歌曲") else Log.d(TAG, "onError: 播放错误,请播放其他歌曲")
                    Log.e(TAG, "Error Prepare 只能使用log或者通知")
                }
            }
            //onError返回值返回false会触发onCompletionListener，y
            //所以返回false，一般意味着会退出当前歌曲播放。
            //如果不想退出当前歌曲播放则应该返回true
            return true
        }
    }

    private inner class onPreparedListener : MediaPlayer.OnPreparedListener {
        override fun onPrepared(mp: MediaPlayer) {
            Log.e(TAG, "onPrepared: $mCurrentPosition")
            if (mCurrentPosition > 0) mMediaPlayer!!.seekTo(mCurrentPosition) else if (mCurrentPosition == -1) resetCurrentPosition()
            //获得音频焦点
            checkFocusPlay(null)
            if (isFirstPlay) {
                isFirstPlay = false
            }
        }
    }

    private fun checkFocusPlay(bundle: Bundle?) {
        val audioFocusState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMyAudioManager!!.registerAudioFocus()
        } else {
        }
        if (audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mMediaPlayer!!.start()
            //更新状态, 使该通知处于栈顶, 赋予播放进度初始值, 从而在通知中展示播放进度
            mMediaSession!!.setPlaybackState(newPlaybackState(PlaybackStateCompat.STATE_PLAYING, bundle))
            mNotificationListener!!.onUpdateNotification()
        } else if (audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_FAILED) { //请求焦点失败
            Toast.makeText(mApplication, "请求播放失败", Toast.LENGTH_SHORT).show()
        } else Log.e(TAG, "checkFocusPlay: 音频焦点延迟获得！")
    }

    //***************************获取新播放状态PlaybackStateCompat****************************/
    fun newPlaybackState(@PlaybackStateCompat.State newState: Int, bundle: Bundle?): PlaybackStateCompat {
        return PlaybackStateCompat.Builder()
            .setExtras(bundle) //设置需使用的Action
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or  //关闭Notification
                    PlaybackStateCompat.ACTION_STOP or  //歌词action，翻译为字幕
                    PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED or  //歌曲收藏action，翻译为星级评级
                    PlaybackStateCompat.ACTION_SET_RATING or  //播放模式切换action，翻译为设置重复播放
                    PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)
            .addCustomAction(CUSTOM_ACTION_PLAYBACK_MODE_CHANGE, playbackMode.toString(), R.drawable.iv_playback_mode_order)
            .setState(newState, mCurrentPosition.toLong(), 1.0f).build()
    }

    //***************************播放模式更改与获取****************************/
    fun setPlayBackMode(mode: Int) {
        DYQL_PLAYBACK_MODE = if (mode < 1 || mode > 3) 1 else mode
        Log.e(TAG, "setPlayBackMode: $mode")
    }

    val playbackMode: Int
        get() {
            Log.e(TAG, "getPlaybackMode: $DYQL_PLAYBACK_MODE")
            return DYQL_PLAYBACK_MODE
        }

    fun playbackModeChange(): Int {
        if (DYQL_PLAYBACK_MODE != dyqlPlaybackModeRepeat) {
            DYQL_PLAYBACK_MODE++
        } else DYQL_PLAYBACK_MODE = dyqlPlaybackModeOrder
        val settings: SharedPreferences = mApplication!!.getSharedPreferences("UserLastMusicPlay", 0)
        val editor: SharedPreferences.Editor = settings.edit()
        editor.putInt("MusicPlaybackMode", DYQL_PLAYBACK_MODE)
        editor.apply()
        return DYQL_PLAYBACK_MODE
    }

    companion object {
        private const val TAG = "MediaPlayManager"
        const val dyqlPlaybackModeOrder = 1
        const val dyqlPlaybackModeRepeat = 3
        const val dyqlPlaybackModeRandom = 2
        const val CUSTOM_ACTION_PLAYBACK_MODE_CHANGE = "playback_mode_change_dyql"
    }

    init {
        mMediaSession = mediaSession
        mNotificationListener = notificationListener
        //初始化MediaPlayer
        mMediaPlayer = MediaPlayer()
        //唤醒锁定模式，关闭屏幕时，CPU不休眠
        mMediaPlayer?.setWakeMode(mApplication, PowerManager.PARTIAL_WAKE_LOCK)
        //初始化MyAudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMyAudioManager = MyAudioManager(mApplication!!, focusChangeListener,
                mMediaPlayer!!.getAudioSessionId())
        }
        mMediaPlayer!!.setAudioAttributes(mMyAudioManager!!.playbackAttributes)
        mMediaPlayer!!.setOnErrorListener(onErrorListener())
        mMediaPlayer!!.setOnPreparedListener(onPreparedListener())
        mMediaPlayer!!.setOnCompletionListener(onCompleteListener())
    }
}