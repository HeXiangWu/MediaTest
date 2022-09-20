package com.example.mediasessiontest.service.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.widget.RemoteViews
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaDescriptionCompat
import android.graphics.Bitmap
import com.example.mediasessiontest.util.PictureUtil
import android.os.Build
import com.example.mediasessiontest.service.manager.MediaPlayerManager
import com.example.mediasessiontest.R
import androidx.annotation.RequiresApi
import android.app.NotificationChannel
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mediasessiontest.service.BaseMusicService
import androidx.core.content.res.ResourcesCompat
import com.example.mediasessiontest.view.SongLrcActivity
import java.lang.ref.SoftReference

/**
 * @since : 2021/11/3
 * 作用:
 */
class MediaNotificationManager(application: Application) {
    private val mPlayAction: NotificationCompat.Action
    private val mPauseAction: NotificationCompat.Action
    private val mNextAction: NotificationCompat.Action
    private val mPrevAction: NotificationCompat.Action
    private val mOrderAction: NotificationCompat.Action
    private val mRepeatAction: NotificationCompat.Action
    private val mRandomAction: NotificationCompat.Action
    private val mLrcAction: NotificationCompat.Action
    private val mLoveAction: NotificationCompat.Action
    val notificationManager: NotificationManager
    private var mApplication: Application?
    private val clickPendingIntent: PendingIntent
    private val deletePendingIntent: PendingIntent
    var isCustomNotification = false
    private val mRemoteViews: SoftReference<RemoteViews>?
    private val mRemoteViewsBig: SoftReference<RemoteViews>?
    fun onDestroy() {
        if (mApplication != null) mApplication = null
        mRemoteViews?.clear()
        mRemoteViewsBig?.clear()
    }

    private fun getPendingIntent(context: Context?, action: String): PendingIntent {
        return PendingIntent.getBroadcast(context!!.applicationContext,
            0, Intent(action), PendingIntent.FLAG_IMMUTABLE)
    }

    /** 获得MediaButtonReceiver的PendingIntent
     * 可进入其中查看源码收录了哪些Action，[PlaybackStateCompat[toKeyCode方法]]
     * 其action未能满足现阶段音频App的全部要求，故未采用，所以走的接收蓝牙广播的回调通道，
     * 且此方法需在AndroidManifest.xml中声明静态广播接收器才能够有播放控制按键回调 */
    @Deprecated("")
    private fun getMediaButtonIntent(application: Application,
                                     @PlaybackStateCompat.Actions state: Long): PendingIntent {
        return MediaButtonReceiver.buildMediaButtonPendingIntent(application, state)
    }

    @SuppressLint("LongLogTag")
    fun getNotification(metadata: MediaMetadataCompat,
                        state: PlaybackStateCompat,
                        token: MediaSessionCompat.Token): Notification {
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val description = metadata.description
        Log.w(TAG, "getNotification: $description, 播放状态 $isPlaying")
        var bitmap = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
        bitmap = PictureUtil.getResIdBitmap(bitmap, 500, mApplication!!.resources,
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) 10 else 100)
        val builder = buildNotification(state, token, isPlaying, metadata, bitmap)
            .setLargeIcon(bitmap)
        Log.e(TAG, "getNotification: $isCustomNotification")
        return builder.build()
    }

    @SuppressLint("LongLogTag")
    private fun buildNotification(state: PlaybackStateCompat,
                                  token: MediaSessionCompat.Token,
                                  isPlaying: Boolean,
                                  metadata: MediaMetadataCompat,
                                  bitmap: Bitmap): NotificationCompat.Builder {
        // Android 8.0+ | Api 26+ 必须为 Notification 创建 通知通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(true)
        }
        var modeAction: NotificationCompat.Action? = null
        //根据播放模式值 确定使用哪个模式Action
        for (customAction in state.customActions) {
            val action = customAction.action
            val mode = customAction.name.toString().toInt()
            if (MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE == action) {
                modeAction = if (1 == mode) {
                    mOrderAction
                } else if (2 == mode) {
                    mRandomAction
                } else {
                    mRepeatAction
                }
            }
            Log.d(TAG, "buildNotification: " + customAction.action + " " + customAction.name)
        }
        val description = metadata.description
        val builder = NotificationCompat.Builder(mApplication!!, CHANNEL_ID)
            .setChannelId(CHANNEL_ID)
            .setContentIntent(clickPendingIntent) //适用于Api21-
            .setDeleteIntent(deletePendingIntent)
            .setSmallIcon(R.drawable.ic_test) //设置 Ongoing 为true ,则此通知不可滑动关闭。
            //设置 Ongoing 为false ,则此通知在音乐暂停或停止时可向右滑动关闭。
            //个人猜测是通过MediaSession播放状态来判定的，
            //所以false 值 适合系统样式的音乐播放控制通知，适用于Api 21+
            //true 值 适合App个性样式的音乐播放控制通知
            .setOngoing(isCustomNotification)
            .setAutoCancel(false) //设置为任意页面可见
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //显示此通知的创建时间
            .setShowWhen(false) //.setWhen(System.currentTimeMillis())
            //通知类别：用于播放的媒体传输控制。MediaSession框架
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT) //添加应用程序图标并设置其强调色，注意颜色(建议支持夜间模式)
            //.setColor(ContextCompat.getColor(mApplication, R.color.notification_bg))
            // Title - 通常指歌曲名。
            .setContentTitle(description.title) // Subtitle - 本APP指 “歌手 - 歌曲专辑名“ 格式文本。
            .setContentText(description.subtitle.toString() + " - " + description.description)
        val style = if (!isCustomNotification) androidx.media.app.NotificationCompat.MediaStyle() else androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
        builder.setStyle(style.setMediaSession(token) //折叠通知时选择显示哪三个Action及其图标，开发人员可自定义，亦可交给用户于设置中选择
            .setShowActionsInCompactView(0, 1, 2) //与Android L | Api 21及更早版本向后兼容。
            .setShowCancelButton(true)
            .setCancelButtonIntent(deletePendingIntent))
        //为Notification.MediaStyle 设置按钮Action，从左至右 0，1，2，3，4
        builder.addAction(modeAction ?: mOrderAction)
            .addAction(if (isPlaying) mPauseAction else mPlayAction)
            .addAction(mNextAction)
            .addAction(mLoveAction)
            .addAction(mLrcAction)
        if (isCustomNotification) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) builder.setStyle(null)
            //专辑图片
            mRemoteViews!!.get()!!.setImageViewBitmap(R.id.notification_iv_album, bitmap)
            mRemoteViewsBig!!.get()!!.setImageViewBitmap(R.id.notification_iv_album, bitmap)
            updateRemoteView(metadata, isPlaying, mApplication)
            builder.setCustomContentView(mRemoteViews.get())
            builder.setCustomBigContentView(mRemoteViewsBig.get())
        }
        return builder
    }

    @SuppressLint("LongLogTag")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(isPlayControl: Boolean) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val importance = if (isPlayControl) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_DEFAULT
            // 给用户展示的通知通道名称.
            val name: CharSequence = "音乐播放"
            // 给用户展示通知通道的描述。
            val description = "Bilibili喜闻人籁 - 音乐播放控制通知"
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            // Configure the notification channel.
            channel.description = description
            //是否显示在锁屏上
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            //关闭此通知发出时铃声
            channel.setSound(null, null)
            /*如果设备支持此功能，则设置发送到此频道通知的通知灯颜色。*/if (importance == NotificationManager.IMPORTANCE_LOW) {
                channel.enableVibration(false) //禁止震动
                channel.vibrationPattern = longArrayOf(0)
            } else {
                channel.enableLights(true) //显示通知呼吸灯
                channel.lightColor = Color.GREEN
                channel.enableVibration(true)
                channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            /*设置发布到此频道的通知是否可以在启动程序中显示为应用程序图标徽章。*/channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "createChannel: 创建新的通知通道 | 信道")
        } else {
            Log.d(TAG, "createChannel: 复用现有通知通道 | 信道")
        }
    }

    private fun updateRemoteView(metadata: MediaMetadataCompat,
                                 isPlaying: Boolean,
                                 application: Application?) {
        val view = mRemoteViews!!.get()
        val viewBig = mRemoteViewsBig!!.get()
        val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        val album = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

        //歌名
        view!!.setTextViewText(R.id.notification_top_song, title)
        viewBig!!.setTextViewText(R.id.notification_top_song, title)
        //歌手
        view.setTextViewText(R.id.notification_top_singer, "$artist - $album")
        viewBig.setTextViewText(R.id.notification_top_singer, "$artist - $album")
        //播放
        val playIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_PLAY)
        val pauseIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_PAUSE)
        /*
        * ！！！通过BitmapFactory获取图片可能无法获取到Bitmap，请使用ResourcesCompat获取Drawable来转Bitmap
        * */
        val playResId = if (isPlaying) R.drawable.iv_lrc_pause else R.drawable.iv_lrc_play
        val drawable = ResourcesCompat.getDrawable(
            application!!.resources, playResId, null)
        if (drawable != null) {
            val playBitmap = PictureUtil.drawableToBitmap(drawable)
            //Log.d(TAG, "updateRemoteView: 播放图片对象是否为空 "+(application.getResources() == null));
            view.setImageViewBitmap(R.id.notification_iv_play, playBitmap)
            viewBig.setImageViewBitmap(R.id.notification_iv_play, playBitmap)
        }
        view.setOnClickPendingIntent(R.id.notification_iv_play, if (isPlaying) pauseIntent else playIntent)
        viewBig.setOnClickPendingIntent(R.id.notification_iv_play, if (isPlaying) pauseIntent else playIntent)
        //收藏
        //歌词
        //版本适配 按压动画
    }

    private fun initRemote(application: Application) {
        val view = mRemoteViews!!.get()
        val viewBig = mRemoteViewsBig!!.get()
        //设置点击发送广播 大视图
        val nextIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_NEXT)
        val previousIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_PREVIOUS)
        val loveIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_COLLECT_SONGS)
        val lrcIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_SHOW_LYRICS)
        val stopIntent = getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_STOP)
        viewBig!!.setOnClickPendingIntent(R.id.notification_iv_right, nextIntent)
        viewBig.setOnClickPendingIntent(R.id.notification_iv_left, previousIntent)
        viewBig.setOnClickPendingIntent(R.id.notification_iv_love, loveIntent)
        viewBig.setOnClickPendingIntent(R.id.notification_iv_close, stopIntent)
        viewBig.setOnClickPendingIntent(R.id.notification_iv_lrc, lrcIntent)
        //小视图
        view!!.setOnClickPendingIntent(R.id.notification_iv_right, nextIntent)
        view.setOnClickPendingIntent(R.id.notification_iv_love, loveIntent)
        view.setOnClickPendingIntent(R.id.notification_iv_close, stopIntent)
    }

    companion object {
        private const val TAG = "MediaNotificationManager"
        private const val CHANNEL_ID = "com.xwrl.mvvm.demo.channel"
    }

    init {
        mApplication = application
        mRemoteViews = SoftReference(RemoteViews(
            application.packageName, R.layout.layout_notification_normal))
        mRemoteViewsBig = SoftReference(RemoteViews(
            application.packageName, R.layout.layout_notification_big))
        initRemote(application)
        notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        clickPendingIntent = PendingIntent.getActivity(application, 0,
            Intent(application, SongLrcActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        deletePendingIntent = getPendingIntent(application,
            BaseMusicService.DYQL_CUSTOM_ACTION_STOP)
        mPlayAction = NotificationCompat.Action(
            R.drawable.iv_lrc_play,
            application.getString(R.string.label_notification_play),
            getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_PLAY))
        mPauseAction = NotificationCompat.Action(
            R.drawable.iv_lrc_pause,
            application.getString(R.string.label_notification_pause),
            getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_PAUSE))
        mNextAction = NotificationCompat.Action(
            R.drawable.iv_next,
            application.getString(R.string.label_notification_next),
            getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_NEXT))
        mPrevAction = NotificationCompat.Action(
            R.drawable.iv_previous,
            application.getString(R.string.label_notification_previous),
            getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_PREVIOUS))
        mLoveAction = NotificationCompat.Action(
            R.drawable.ic_love,
            application.getString(R.string.label_notification_love),
            getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_COLLECT_SONGS))
        mOrderAction = NotificationCompat.Action(
            R.drawable.iv_playback_mode_order,
            application.getString(R.string.label_notification_mode),
            getPendingIntent(application, MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE))
        mRepeatAction = NotificationCompat.Action(
            R.drawable.iv_playback_mode_repeat,
            application.getString(R.string.label_notification_mode),
            getPendingIntent(application, MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE))
        mRandomAction = NotificationCompat.Action(
            R.drawable.iv_playback_mode_random,
            application.getString(R.string.label_notification_mode),
            getPendingIntent(application, MediaPlayerManager.DYQL_CUSTOM_ACTION_PLAYBACK_MODE_CHANGE))
        mLrcAction = NotificationCompat.Action(
            R.drawable.ic_lrc,
            application.getString(R.string.label_notification_lyric),
            getPendingIntent(application, BaseMusicService.DYQL_CUSTOM_ACTION_SHOW_LYRICS))
        notificationManager.cancelAll()
    }
}