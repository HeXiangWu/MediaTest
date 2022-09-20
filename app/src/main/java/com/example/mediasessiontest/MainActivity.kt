package com.example.mediasessiontest

import com.example.mediasessiontest.viewmodel.MusicViewModel
import android.content.Intent
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.MediaBrowserCompat
import android.os.Bundle
import com.example.mediasessiontest.util.PermissionUtil
import androidx.databinding.DataBindingUtil
import com.example.mediasessiontest.R
import com.example.mediasessiontest.service.MusicService
import com.example.mediasessiontest.MainActivity
import com.example.mediasessiontest.service.BaseMusicService
import androidx.annotation.RequiresApi
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.mediasessiontest.view.MusicActivity
import com.example.mediasessiontest.view.SongLrcActivity
import com.example.mediasessiontest.util.PictureUtil
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.Toast
import android.view.View.OnLongClickListener
import com.example.mediasessiontest.databinding.ActivityMainBinding
import java.util.*

class MainActivity : BaseActivity<MusicViewModel?>() {
    private var mMainBinding: ActivityMainBinding? = null
    private var mMusicViewModel: MusicViewModel? = null
    private var mTimer: Timer? = null
    private var mIntentMusic: Intent? = null
    override val controllerCallback: MediaControllerCompat.Callback
        protected get() = MyMediaControllerCallback()
    override val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback
        protected get() = MyMediaBrowserSubscriptionCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (PermissionUtil.IsPermissionNotObtained(this)) {
            PermissionUtil.getStorage(this)
        }
        super.onCreate(savedInstanceState)
        mMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mMusicViewModel = MusicViewModel(application)
        mMainBinding?.setUserInfo(mMusicViewModel)
        super.setBackToDesktop()
        initView()
        mIntentMusic = Intent(this, MusicService::class.java)
        startService(mIntentMusic)
    }

    override fun onStart() {
        super.onStart()
        UpdateProgressBar()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        StopProgressBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mIntentMusic != null) {
            mIntentMusic = null
        }
        if (mMusicViewModel != null) {
            mMusicViewModel = null
        }
        if (mMainBinding != null) {
            mMainBinding!!.unbind()
            mMainBinding = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PermissionUtil.REQUEST_PERMISSION_CODE) {
            if (PermissionUtil.IsPermissionNotObtained(this)) {
                PermissionUtil.getStorage(this)
            } else {
                Log.w(TAG, "onRequestPermissionsResult: 已获取读写权限")
                //添加列表
                super.subscribe()
            }
        }
    }

    private inner class MyMediaBrowserSubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onChildrenLoaded(parentId: String,
                                      children: List<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            activityOnChildrenLoad(mMusicViewModel,
                mMainBinding!!.mainActivityIvPlayLoading,
                children)
            mMusicViewModel!!.setPhoneRefresh(mRefreshRateMax)
            //！！！少更新样式状态
            mMusicViewModel!!.isCustomStyle = MediaControllerCompat.getMediaController(this@MainActivity)
                .metadata.getLong(BaseMusicService.DYQL_NOTIFICATION_STYLE) == 0L
        }

        override fun onError(parentId: String) {
            super.onError(parentId)
        }
    }

    private inner class MyMediaControllerCallback : MediaControllerCompat.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            super.onMetadataChanged(metadata)
            mMusicViewModel!!.SyncMusicInformation()
        }

        override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
            super.onPlaybackStateChanged(playbackState)
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
            mMusicViewModel!!.setPlaybackState(playbackState.state)
            playbackStateChanged(playbackState,
                mMainBinding!!.mainActivityIvPlayLoading)
        }

        override fun onSessionEvent(event: String, extras: Bundle) {
            super.onSessionEvent(event, extras)
        }
    }

    private fun initView() {
        mMainBinding!!.activityMainUiRoot.setOnApplyWindowInsetsListener(this)
        //等信息更新后再设置回调
        mMainBinding!!.activityMainGridLayout.setOnClickListener { v: View? -> startActivity(Intent(this@MainActivity, MusicActivity::class.java)) }
        mMainBinding!!.mainActivityBottomLayout.setOnClickListener { v: View? ->
            startActivity(Intent(this@MainActivity, SongLrcActivity::class.java))
            overridePendingTransition(R.anim.push_in, 0)
        }
        mMainBinding!!.mainActivityBottomProgressBar.setOnClickListener { v: View? -> mMusicViewModel!!.playbackButton() }
        super.initAnimation(mMainBinding!!.mainActivityBottomIvAlbum)
        mMainBinding!!.activityMainIvUser.setImageDrawable(
            PictureUtil.createUserIconDrawable(application,
                BitmapFactory.decodeResource(resources, R.drawable.baseui_user_default),
                120, dpToPx(64)))
        mMainBinding!!.activityMainTopLayout.setOnClickListener { v: View? -> Toast.makeText(this, "打开APP菜单设置", Toast.LENGTH_SHORT).show() }
        //设置深色模式适配的颜色
        val color = super.viewColor
        mMainBinding!!.mainActivityBottomIvList.drawable.setTint(color)
        mMainBinding!!.mainActivityBottomProgressBar.setProgressColor(color)
    }

    private fun UpdateProgressBar() {
        if (mTimer != null) {
            return
        }
        mTimer = Timer()
        mTimer!!.schedule(mMusicViewModel!!.circleBarTask, 300, 300)
    }

    private fun StopProgressBar() {
        if (mTimer != null) {
            mTimer!!.purge()
            mTimer!!.cancel()
            mTimer = null
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}