package com.example.mediasessiontest.service.manager

import android.app.Application
import android.content.Context
import androidx.annotation.RequiresApi
import android.os.Build
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioManager
import android.view.WindowManager
import android.net.wifi.WifiManager.WifiLock
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.audiofx.LoudnessEnhancer
import android.net.wifi.WifiManager
import android.util.Log
import java.util.*
import java.util.function.Function

/**
 * 作用: 系统声音服务 所需 代码和方法  管理帮助类
 * 1.音频焦点管理、 顺便管理下Wifi锁
 * 2.调节系统音量、
 * 3.播放声音增强、
 */
class MyAudioManager @RequiresApi(api = Build.VERSION_CODES.O) constructor(application: Application,
                                                                           focusChangeListener: OnAudioFocusChangeListener?,
                                                                           audioSessionId: Int) {
    private var mApplication: Application?
    private var mAudioManager: AudioManager?
    private var mWindowManager: WindowManager?
    private var mWifiLock: WifiLock?

    //音频焦点管理
    private var mFocusChangeListener: OnAudioFocusChangeListener?
    var playbackAttributes: AudioAttributes?
        private set
    private var mFocusRequest: AudioFocusRequest?

    //人声增强
    private var mLoudnessEnhancer: LoudnessEnhancer?

    //获取与设置用户信息-人声增强幅度
    var currentVoiceMb: Long = 0
        private set

    @RequiresApi(Build.VERSION_CODES.O)
    fun onDestroy() {
        releaseAudioFocus()
        if (mApplication != null) mApplication = null
        if (mAudioManager != null) mAudioManager = null
        if (mWindowManager != null) mWindowManager = null
        if (mWifiLock != null) mWifiLock = null
        if (mLoudnessEnhancer != null) {
            mLoudnessEnhancer!!.release()
            mLoudnessEnhancer = null
        }
        if (mFocusChangeListener != null) mFocusChangeListener = null
        if (playbackAttributes != null) playbackAttributes = null
        if (mFocusRequest != null) mFocusRequest = null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun lowerTheVolume() {
        val volume = volume
        setVolume(if (volume > 4) volume - 2 else 2)
    }

    val maxVolume: Int
        get() = if (mAudioManager != null) {
            mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        } else 0

    fun setVolume(volume: Int): Boolean {
        val canSetVolume = mAudioManager != null && !mAudioManager!!.isVolumeFixed
        if (canSetVolume) {
            mAudioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND)
            //Log.d(TAG, "setVolume: "+percent);
        } else Log.e(TAG, "setVolume: 音量设置无效 参数 $volume")
        return canSetVolume
    }

    @get:RequiresApi(api = Build.VERSION_CODES.N)
    val volume: Int
        get() {
            if (mAudioManager != null) {
                val `object` = getVolume(mAudioManager!!)
                if (`object` is Int) {
                    return `object`.toString().toInt()
                }
            }
            return 0
        }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun getVolume(manager: AudioManager): Any {
        return Optional.of(manager).map(Function<AudioManager, Any> { manager1: AudioManager -> manager1.getStreamVolume(AudioManager.STREAM_MUSIC) }).orElse("0")
    }

    fun setCurrentVoiceMb(currentVoiceMb: Long, isSave: Boolean) {
        if (this.currentVoiceMb == currentVoiceMb) return
        if (mLoudnessEnhancer != null) {
            mLoudnessEnhancer!!.enabled = currentVoiceMb > 0
            mLoudnessEnhancer!!.setTargetGain(currentVoiceMb.toInt())
        }
        if (!isSave) return
        this.currentVoiceMb = currentVoiceMb
        if (this.currentVoiceMb < 0) this.currentVoiceMb = 0
        if (this.currentVoiceMb > 2600) this.currentVoiceMb = 2600

        /*settings = getSharedPreferences("UserLastMusicPlay",0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("UserVoiceMb",mCurrentVoiceMb);
        editor.apply();*/
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun registerAudioFocus(): Int {
        //启动wifi锁,在暂停或者停止时释放WiFi锁
        mWifiLock!!.acquire()
        //获得播放焦点
        return mAudioManager!!.requestAudioFocus(mFocusRequest!!)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun releaseAudioFocus() {
        //停止播放音乐后释放焦点
        mAudioManager!!.abandonAudioFocusRequest(mFocusRequest!!)
        //释放wifi锁
        if (mWifiLock!!.isHeld) mWifiLock!!.release()
    }

    companion object {
        private const val TAG = "MyAudioManager"

        //音量Key
        const val DYQL_CUSTOM_ACTION_MAX_VOLUME = "max_volume_dyql"
        const val DYQL_CUSTOM_ACTION_CURRENT_VOLUME = "current_volume_dyql"
    }

    init {
        mApplication = application

        //初始化管理者
        mAudioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mWindowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mWifiLock = (application.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL, "dyqlLock")

        //音频焦点管理初始化
        mFocusChangeListener = focusChangeListener
        playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        //获取长时间音频播放焦点
        mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            // 在焦点锁定时发出的请求会返回 AUDIOFOCUS_REQUEST_DELAYED。
            // 当锁定音频焦点的情况不再存在时（例如当通话结束时），
            // 系统会批准待处理的焦点请求，并调用 onAudioFocusChange() 来通知您的应用。
            .setAcceptsDelayedFocusGain(true) //播放通知铃声时自动降低音量，true则回调音频焦点更改回调，可在回调里暂停音乐
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(mFocusChangeListener!!)
            .build()

        //人声增强器初始化
        mLoudnessEnhancer = LoudnessEnhancer(audioSessionId)
        mLoudnessEnhancer!!.setTargetGain(1000) //调节此值 可按值增强声音 | 人声增强 mLoudnessEnhancer
        mLoudnessEnhancer!!.enabled = true
    }
}