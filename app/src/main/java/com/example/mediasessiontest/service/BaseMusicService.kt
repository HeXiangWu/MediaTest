package com.example.mediasessiontest.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.media.MediaBrowserServiceCompat

/**
 *
 * @author: tunjiang
 * @email: wuhexiang@bilibili.com
 * @date: 2022/8/12
 * @Desc:
 */
abstract class BaseMusicService : MediaBrowserServiceCompat() {

    private val TAG: String = BaseMusicService::class.java.simpleName

    private val mMediaController: MediaControllerCompat? = null
    private val isStartForeground = false

    override fun onCreate() {
        super.onCreate()
        initManager()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        TODO("Not yet implemented")
    }

    open fun initManager() {
        //初始化通知管理者和媒体按钮接收器
        val settings = getSharedPreferences("UserLastMusicPlay", 0)
        val notificationStyle = settings.getBoolean("NotificationStyle", false)
        //Log.e(TAG, "initManager: "+notificationStyle);
    }
}