package com.example.mediasessiontest

import com.example.mediasessiontest.util.ImmersiveStatusBarUtil.transparentBar
import androidx.appcompat.app.AppCompatActivity
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.animation.Animation
import android.animation.ObjectAnimator
import android.os.Bundle
import android.media.AudioManager
import android.view.WindowInsets
import android.content.ComponentName
import com.example.mediasessiontest.service.MusicService
import android.view.animation.LinearInterpolator
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import android.os.Build
import android.os.RemoteException
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.util.TypedValue
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.example.mediasessiontest.viewmodel.BaseViewModel
import java.lang.ref.SoftReference

/**
 * 作用:
 */
abstract class BaseActivity<M : BaseViewModel?> : AppCompatActivity(), View.OnApplyWindowInsetsListener {
    private var mMediaBrowser: MediaBrowserCompat? = null
    private var mControllerCallback: MediaControllerCompat.Callback? = null
    private var mSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback? = null

    //动画
    protected var loadingAnimation: Animation? = null
        private set
    private var mRecordAnimator: ObjectAnimator? = null

    //设备参数
    protected var mRefreshRateMax = 0
    protected var mPhoneWidth = 0
    protected var mPhoneHeight = 0
    protected var isPad = false
    protected var backToDesktop = false
    protected var isLifePauseAnimator = false
    protected var isFirstResume = false
    protected abstract val controllerCallback: MediaControllerCompat.Callback?
    protected abstract val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback?
    override fun onCreate(savedInstanceState: Bundle?) {
        transparentBar(this, false)
        super.onCreate(savedInstanceState)
        HighHzAdaptation()
        initMediaBrowser()
        initRotateAnimation()
        Log.d(TAG, "onCreate: ")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
        if (!mMediaBrowser!!.isConnected) {
            mMediaBrowser!!.connect()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        volumeControlStream = AudioManager.STREAM_MUSIC
        checkAnimator()
        isFirstResume = true
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
        checkAnimator()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        releaseMediaBrowser()
        if (loadingAnimation != null) {
            loadingAnimation!!.reset()
            loadingAnimation!!.cancel()
            loadingAnimation = null
        }
        if (mRecordAnimator != null) {
            mRecordAnimator!!.pause()
            mRecordAnimator!!.cancel()
            mRecordAnimator = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        Log.d(TAG, "onApplyWindowInsets: " + insets.systemWindowInsetTop)
        if (insets.systemWindowInsetBottom < 288) {
            val paddingBottom = insets.systemWindowInsetBottom
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, paddingBottom)
        }
        return insets
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            onFinish()
        } else super.onKeyDown(keyCode, event)
    }

    private fun initMediaBrowser() {
        mControllerCallback = controllerCallback
        mSubscriptionCallback = subscriptionCallback
        subscriptionCallback
        mMediaBrowser = MediaBrowserCompat(this,
            ComponentName(this, MusicService::class.java),
            MyMediaBrowserConnectionCallback(), null)
    }

    private fun initRotateAnimation() {
        //加载效果 旋转动画 初始化
        loadingAnimation = SoftReference(
            AnimationUtils.loadAnimation(this, R.anim.circle_rotate)).get()
        val interpolator = SoftReference(LinearInterpolator()).get()
        loadingAnimation!!.interpolator = interpolator
    }

    protected fun initAnimation(view: View?): ObjectAnimator? {
        //唱片转动动画 初始化
        mRecordAnimator = ObjectAnimator.ofFloat(
            view, "rotation", 0.0f, 360.0f)
        mRecordAnimator?.setDuration(30000) //设定转一圈的时间
        mRecordAnimator?.setRepeatCount(Animation.INFINITE) //设定无限循环
        mRecordAnimator?.setRepeatMode(ObjectAnimator.RESTART) // 循环模式
        mRecordAnimator?.setInterpolator(LinearInterpolator()) //匀速
        return mRecordAnimator
    }

    private fun releaseMediaBrowser() {
        disConnect()
        if (mControllerCallback != null) {
            mControllerCallback = null
        }
        if (mSubscriptionCallback != null) {
            mSubscriptionCallback = null
        }
        if (mMediaBrowser != null) {
            mMediaBrowser = null
        }
    }

    //断开连接
    private fun disConnect() {
        if (MediaControllerCompat.getMediaController(this@BaseActivity) != null) {
            MediaControllerCompat.getMediaController(this@BaseActivity).unregisterCallback(mControllerCallback!!)
        }
        if (mMediaBrowser!!.isConnected) {
            mMediaBrowser!!.unsubscribe(mMediaBrowser!!.root)
            mMediaBrowser!!.disconnect()
        }
    }

    protected fun setBackToDesktop() {
        backToDesktop = true
    }

    private inner class MyMediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Log.d(TAG, "onConnected: 连接成功")

            // 获得MediaSession的Token口令
            val token = mMediaBrowser!!.sessionToken

            // 初始化MediaControllerCompat
            var mediaController: MediaControllerCompat? = null
            try {
                mediaController = MediaControllerCompat(this@BaseActivity, token)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            if (mediaController == null) {
                return
            }
            // 保存controller
            MediaControllerCompat.setMediaController(this@BaseActivity, mediaController)
            //注册controller回调以保持数据同步
            mediaController.registerCallback(mControllerCallback!!)
            subscribe()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            //服务崩溃了。禁用传输控制，直到它自动重新连接
            Log.d(TAG, "onConnectionSuspended: 连接中断")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            //服务端拒绝连接
            Log.d(TAG, "onConnectionFailed: 连接失败")
        }
    }

    protected fun subscribe() {
        if (mMediaBrowser != null && mMediaBrowser!!.isConnected) {
            val mediaId = mMediaBrowser!!.root
            mMediaBrowser!!.unsubscribe(mediaId)
            //向服务订阅音乐列表集合信息！
            mMediaBrowser!!.subscribe(mediaId, mSubscriptionCallback!!)
        }
    }

    protected fun onFinish(): Boolean {
        if (backToDesktop) {
            val home = Intent(Intent.ACTION_MAIN)
            home.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            home.addCategory(Intent.CATEGORY_HOME)
            startActivity(home)
        } else {
            finish()
            overridePendingTransition(0, R.anim.push_out)
        }
        return true
    }

    private fun checkAnimator() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        if (!isFirstResume || mRecordAnimator == null || mediaController == null) {
            return
        }
        val state = mediaController.playbackState.state
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            isLifePauseAnimator = if (isLifePauseAnimator) {
                mRecordAnimator!!.resume()
                false
            } else {
                mRecordAnimator!!.pause()
                true
            }
        }
    }

    protected fun playbackStateChanged(playbackState: PlaybackStateCompat?,
                                       loadingView: View) {
        if (mRecordAnimator == null || playbackState == null) {
            Toast.makeText(this, "未初始化唱片旋转动画", Toast.LENGTH_SHORT).show()
            return
        }
        val state = playbackState.state
        val bundle = playbackState.extras
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            Log.w(TAG, "playbackStateChanged: ")
            loadingView.clearAnimation()
            loadingView.visibility = View.GONE
            if (bundle == null || !mRecordAnimator!!.isStarted) mRecordAnimator!!.start() //动画开始
            else if (bundle.getBoolean("Continue_Playing_Tips")) mRecordAnimator!!.resume()
        } else if (state == PlaybackStateCompat.STATE_BUFFERING) {
            loadingView.startAnimation(loadingAnimation)
            loadingView.visibility = View.VISIBLE
        } else if (state == PlaybackStateCompat.STATE_PAUSED) {
            mRecordAnimator!!.pause()
        }
    }

    /**
     * 设置最大刷新率
     * 1.通过Activity 的Window对象获取到[Display.Mode[]] 所有的刷新率模式数组
     * 2.通过遍历判断出刷新率最大那一组，并获取此组引用[Display.Mode]
     * 3.国际惯例首先判空，再获取[WindowManager.LayoutParams]引用，其成员变量preferredDisplayModeId是[Display.Mode]的ModeID
     * 4.window.setAttributes(layoutParams);最后设置下，收工 */
    protected fun HighHzAdaptation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 获取系统window支持的模式,得到刷新率组合
            val window = window
            //获取屏幕宽高
            val dm = DisplayMetrics()
            window.windowManager.defaultDisplay.getMetrics(dm)
            mPhoneWidth = dm.widthPixels
            mPhoneHeight = dm.heightPixels
            isPad = Math.max(mPhoneWidth, mPhoneHeight).toFloat() /
                Math.min(mPhoneWidth, mPhoneHeight) < 1.5
            //获取屏幕刷新率组合
            val modes = window.windowManager.defaultDisplay.supportedModes
            //对获取的模式，基于刷新率的大小进行排序，从小到大排序
            var RefreshRateMax = 0f
            var RefreshRateMaxMode: Display.Mode? = null
            for (mode in modes) {
                val RefreshRateTemp = mode.refreshRate
                if (RefreshRateTemp > RefreshRateMax) {
                    RefreshRateMax = RefreshRateTemp
                    RefreshRateMaxMode = mode
                }
            }
            if (RefreshRateMaxMode != null) {
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = RefreshRateMaxMode.modeId
                window.attributes = layoutParams
                //Log.d(TAG, "设置最大刷新率为 "+RefreshRateMaxMode.getRefreshRate()+"Hz");
                mRefreshRateMax = RefreshRateMax.toInt()
            }
        }
    }

    protected fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics).toInt()
    }

    protected fun activityOnChildrenLoad(m: M, view: View,
                                         children: List<MediaBrowserCompat.MediaItem>) {
        val mediaController = MediaControllerCompat.getMediaController(this)
        m!!.setMediaControllerCompat(mediaController)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            m.SyncMusicInformation()
        }

        //同步播放动画
        val playbackState = mediaController.playbackState
        m.setPlaybackState(playbackState.state)
        playbackStateChanged(playbackState, view)
        //添加列表
        updateMusicList(mediaController, children)
    }

    private fun updateMusicList(mediaController: MediaControllerCompat?,
                                children: List<MediaBrowserCompat.MediaItem>) {
        if (mediaController != null && mediaController.queue.size <= 0) {
            for (mediaItem in children) {
                mediaController.addQueueItem(mediaItem.description)
            }
            mediaController.transportControls.prepare()
        }
    }

    /**
     * @return 深色模式适配的颜色
     *
     */
    @get:ColorInt
    protected val viewColor: Int
        protected get() = ResourcesCompat.getColor(resources, R.color.colorNightViewBlack, null)

    companion object {
        private const val TAG = "BaseActivity"
    }
}