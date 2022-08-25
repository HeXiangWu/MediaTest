package com.example.mediasessiontest.service

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.example.mediasessiontest.service.manager.MediaPlayerManager
import com.google.android.gms.cast.framework.MediaNotificationManager

/**
 *
 * @author: tunjiang
 * @email: wuhexiang@bilibili.com
 * @date: 2022/8/12
 * @Desc:
 */
abstract class BaseMusicService : MediaBrowserServiceCompat() {

    private val TAG: String = BaseMusicService::class.java.simpleName

    private var mMediaController: MediaControllerCompat? = null
    private val isStartForeground = false

    companion object {
        val CUSTOM_ACTION_COLLECT_SONGS = "collect_songs_dyql"
        val CUSTOM_ACTION_SHOW_LYRICS = "show_lyrics_dyql"
        val CUSTOM_ACTION_PLAY = "play_dyql"
        val CUSTOM_ACTION_PAUSE = "pause_dyql"
        val CUSTOM_ACTION_PREVIOUS = "previous_dyql"
        val CUSTOM_ACTION_NEXT = "next_dyql"
        val CUSTOM_ACTION_STOP = "stop_dyql"
        val NOTIFICATION_STYLE = "notification_style_dyql"
    }

    override fun onCreate() {
        super.onCreate()
        initManager()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mMediaController != null) {
            mMediaController = null
        }
        if (mBlueToothReceiver != null) {
            unregisterReceiver(mBlueToothReceiver)
        }
    }

    open fun initManager() {
        //初始化通知管理者和媒体按钮接收器
        val settings = getSharedPreferences("UserLastMusicPlay", 0)
        val notificationStyle = settings.getBoolean("NotificationStyle", false)
        //Log.e(TAG, "initManager: "+notificationStyle);
    }

    protected var mBlueToothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mMediaController == null) {
                Log.e(TAG, "onReceive: mMediaController == null")
                return
            }
            val transportControls: MediaControllerCompat.TransportControls = mMediaController!!.getTransportControls()
            val action = intent.action
            Log.d(TAG, "onReceive: $action")
            val bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0)
            Log.d(TAG, "onReceive: $bluetoothState")
            when (bluetoothState) {
                BluetoothAdapter.STATE_DISCONNECTED, BluetoothAdapter.STATE_TURNING_OFF -> {}
                BluetoothAdapter.STATE_TURNING_ON -> {}
                BluetoothAdapter.STATE_CONNECTED -> {}
            }
            if (MediaPlayerManager.CUSTOM_ACTION_PLAYBACK_MODE_CHANGE.equals(action)) {
                transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
            } else if (CUSTOM_ACTION_PLAY == action) {
                transportControls.play()
            } else if (CUSTOM_ACTION_PAUSE == action) {
                transportControls.pause()
            } else if (CUSTOM_ACTION_PREVIOUS == action) {
                transportControls.skipToPrevious()
            } else if (CUSTOM_ACTION_NEXT == action) {
                transportControls.skipToNext()
            } else if (CUSTOM_ACTION_STOP == action) {
                transportControls.stop()
            }
        }
    }
}