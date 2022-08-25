package com.example.mediasessiontest.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import com.example.mediasessiontest.model.BaseModel
import com.example.mediasessiontest.model.MusicModel
import com.example.mediasessiontest.service.manager.LastMetaManager
import com.example.mediasessiontest.service.manager.MediaPlayerManager
import java.util.*

/**
 *
 * @author: tunjiang
 * @email: wuhexiang@bilibili.com
 * @date: 2022/8/12
 * @Desc:
 */
class MusicService : BaseMusicService() {
    private val TAG = "MusicService"
    private val MY_MEDIA_ROOT_ID = "media_root_id"
    private val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

    private var mMediaSession: MediaSessionCompat? = null
    private val mQueueIndex = -1
    private var mMediaPlayerManager: MediaPlayerManager? = null
    private val IS_AUDIO_FOCUS_LOSS_TRANSIENT = false
    private var mLastMetaManager: LastMetaManager? = null
    private var mModel: BaseModel? = null
    private var mTimer: Timer? = null
    private val mPlaylist: MutableList<MediaSessionCompat.QueueItem> = ArrayList()

    private var mMusicList: LinkedHashMap<String, MediaMetadataCompat>? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: ")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMedia()
        Log.d(TAG, "onDestroy: ")
    }

    //************************************本Service处理与客户端的链接**********************************/
    //参考：https://developer.android.google.cn/guide/topics/media-apps/audio-app/building-a-mediabrowserservice
    //************************************本Service处理与客户端的链接**********************************/
    //参考：https://developer.android.google.cn/guide/topics/media-apps/audio-app/building-a-mediabrowserservice
    /** [MediaBrowserServiceCompat.onGetRoot] 控制对服务的访问
     * 1.onGetRoot() 方法返回内容层次结构的根节点。如果该方法返回 null，则会拒绝连接。
     * 2.要允许客户端连接到您的服务并浏览其媒体内容，onGetRoot() 必须返回非 null 的 BrowserRoot，
     * 这是代表您的内容层次结构的根 ID。
     * 3.要允许客户端连接到您的 MediaSession 而不进行浏览，
     * onGetRoot() 仍然必须返回非 null 的 BrowserRoot，但此根 ID 应代表一个空的内容层次结构。
     *
     * 注意：onGetRoot() 方法应该快速返回一个非 null 值。用户身份验证和其他运行缓慢的进程不应在 onGetRoot() 中运行。
     * 大多数业务逻辑应该在 onLoadChildren() 方法中处理。 */
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {

        //（可选）控制指定包名称的访问级别，要做到这一点，您需要编写自己的逻辑。
        return if (allowBrowsing(clientPackageName, clientUid)) { //允许浏览
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            BrowserRoot(MY_MEDIA_ROOT_ID, null)
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierachy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null)
        }
    }

    private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
        return true
    }

    /** [MediaBrowserServiceCompat.onLoadChildren]
     * 使客户端能够构建和显示 内容层次结构菜单,并通过 onLoadChildren() 传达内容
     * 1.客户端连接后，可以通过重复调用 MediaBrowserCompat.subscribe() 来遍历内容层次结构，以构建界面的本地表示方式。
     * subscribe() 方法将回调 onLoadChildren() 发送给服务，该服务会返回 MediaBrowser.MediaItem 对象的列表。
     * 2.每个 MediaItem 都有一个唯一的 ID 字符串，这是一个不透明令牌。当客户端想要打开子菜单或播放某项内容时，
     * 它就会传递此 ID。您的服务负责将此 ID 与相应的菜单节点或内容项关联起来。
     *
     * 注意：MediaBrowserService 传送的 MediaItem 对象不应包含图标位图。
     * 当您为每项内容构建 MediaDescription 时，请通过调用 setIconUri() 来使用 Uri。 */
    override fun onLoadChildren(parentMediaId: String,
                                result: Result<List<MediaBrowserCompat.MediaItem?>?>) {
        //将信息从当前线程中移除，允许后续调用sendResult方法
        result.detach()
        //浏览不被允许
        if (TextUtils.equals(MY_EMPTY_MEDIA_ROOT_ID, parentMediaId)) {
            result.sendResult(null)
            return
        }
        Log.d(TAG, "onLoadChildren: $parentMediaId")
    }

    private fun init() {
        mModel = MusicModel()
        mLastMetaManager = LastMetaManager(application)
        initMediaSession()
    }

    private fun initMediaSession() {
        //初始化MediaSession | 媒体会话
        mMediaSession = MediaSessionCompat(this, packageName)
        mMediaSession!!.isActive = true
        mMediaSession!!.setQueue(mPlaylist)

        // !!!启用来自MediaButtons和TransportControl的回调
        // 1.允许媒体按钮回调：其他蓝牙设备或者安卓智能设备 通过 媒体响应按钮 发送 播放控制消息 给 Service服务
        // 2.允许媒体队列管理：onAddQueueMediaItem()允许队列管理，为执行上、下一曲相关方法
        // 3.允许媒体命令传输：View客户端 播放控制消息 发给 Service服务 执行相关方法
        mMediaSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)


        // 设置会话的令牌，以便客户端活动可以与其通信。
        sessionToken = mMediaSession!!.sessionToken
        initMediaPlayerManager()

        //给MediaSession设置初始状态
        mMediaSession!!.setPlaybackState(
            mMediaPlayerManager!!.newPlaybackState(PlaybackStateCompat.STATE_NONE, null))
    }

    private fun initMediaPlayerManager() {
        if (mMediaPlayerManager != null) {
            return
        }
        //todo
    }

    private fun releaseMediaPlayerManager() {
        if (mMediaSession != null) {
            mMediaSession!!.setPlaybackState(mMediaPlayerManager!!.newPlaybackState(
                PlaybackStateCompat.STATE_STOPPED, null))
            mMediaSession!!.isActive = false
        }
        if (mMediaPlayerManager != null) {
            mMediaPlayerManager!!.onDestroy()
            mMediaPlayerManager = null
        }
        if (mLastMetaManager != null) {
            mLastMetaManager!!.onDestroy()
            mLastMetaManager = null
        }
    }

    private fun releaseMedia() {
        mPlaylist.clear()
        if (mMusicList != null) {
            if (mMusicList!!.size > 0) {
                mMusicList!!.clear()
            }
            mMusicList = null
        }
        releaseMediaPlayerManager()
        if (mMediaSession != null) {
            mMediaSession!!.release()
            mMediaSession = null
        }
    }

    //**********************************************Metadata元数据相关方法***************************/
    private fun getMediaItems(musicMaps: LinkedHashMap<String, MediaMetadataCompat>): List<MediaBrowserCompat.MediaItem?>? {
        val result: MutableList<MediaBrowserCompat.MediaItem?> = ArrayList()
        for (metadata: MediaMetadataCompat in musicMaps.values) {
            result.add(
                MediaBrowserCompat.MediaItem(
                    metadata.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            /*Log.d(TAG, "getMediaItems: "+metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)+
                    " 键值 "+metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));*/
        }
        Log.d(TAG, "getMediaItems: " + result.size)
        return result
    }

}