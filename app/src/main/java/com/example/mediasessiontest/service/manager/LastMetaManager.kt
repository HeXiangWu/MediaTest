package com.example.mediasessiontest.service.manager

import android.app.Application
import android.content.SharedPreferences
import com.example.mediasessiontest.service.manager.LastMetaManager
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import java.lang.ref.SoftReference

/**
 * 作用:
 */
class LastMetaManager(application: Application) {
    private val settings: SoftReference<SharedPreferences?>?
    fun get(): SharedPreferences? {
        return settings?.get()
    }

    private fun checkSetting(): Boolean {
        val isNull = settings == null || settings.get() == null
        if (isNull) Log.e(TAG, "checkSetting: 为空！")
        return isNull
    }

    fun saveMusicPosition(position: Int) {
        if (!checkSetting()) {
            val editor = get()!!.edit()
            editor.putInt("MusicPosition", position)
            editor.apply()
        }
    }

    val lastMusicPosition: Int
        get() = get()!!.getInt("MusicPosition", 0)

    fun getLastPlaybackMode(defaultMode: Int): Int {
        return get()!!.getInt("MusicPlaybackMode", defaultMode)
    }

    val lastAlbumPath: String?
        get() = get()!!.getString("MusicAlbumPath", "")
    val notificationStyle: Boolean
        get() = get()!!.getBoolean("NotificationStyle", false)

    fun saveNotificationStyle(isCustomNotificationStyle: Boolean) {
        val editor = get()!!.edit()
        editor.putBoolean("NotificationStyle", isCustomNotificationStyle)
        editor.apply()
    }
    //Log.d(TAG, "GetLastMusicPlay: "+title);

    //mCurrentDuration = settings.getLong("MusicDuration",0);
    /*mLastAlbumPath = settings.getString("LastAlbumPath","");
     mNextAlbumPath = settings.getString("NextAlbumPath","");
     mMusicQueueIndex = settings.getInt("MusicQueue",0);
     mLastQueueIndex = settings.getInt("MusicLastQueue",-1);
     mNextQueueIndex = settings.getInt("MusicNextQueue",-1);
     mCurrentSource = settings.getString("MusicSourceAlias","本地播放");
     mUserName = settings.getString("UserName","用户名");     //用户个性名称
     mUserLabel = settings.getString("UserLabel","Supreme"); //用户个性便笺
     mUserIconPath = settings.getString("IconPath","none");  //用户头像Url
     mCurrentBlurs = settings.getFloat("UserSetBlurs",13f);  //用户设置的模糊值
     mCurrentCycleTime = settings.getLong("UserCycleTime",36000);  //用户设置的唱片周期
     mCurrentVoiceMb = settings.getLong("UserVoiceMb",1000);  //用户设置的人声增强（mb）1000mb = 10db
     mCurrentFileMusicTitle = settings.getString("FileMusicTitle","");//默认为顺序播放
     mCurrentFileMusicArtist = settings.getString("FileMusicArtist","");//默认为顺序播放
     isNotificationMediaStyle = settings.getBoolean("NotificationStyle",true);
     if(!UsersGuidePermissionsUtil.checkCanDrawOverlays(this.getApplicationContext()))
         isLrcViewShow = settings.getBoolean("LrcShow",false);*/
    val lastMusicPlay: MediaMetadataCompat.Builder
        get() {
            val title: String
            val artist: String
            val album: String
            val path: String
            val albumPath: String
            val setting = get()
            title = setting!!.getString("MusicTitle", "").toString()
            artist = setting.getString("MusicArtist", "")
                ?.replace("&".toRegex(), "/").toString()
            album = setting.getString("MusicAlbum", "").toString()
            path = setting.getString("MusicPath", "").toString()
            albumPath = setting.getString("MusicAlbumPath", "").toString()
            val duration = setting.getLong("MusicDuration", 0)
            //Log.d(TAG, "GetLastMusicPlay: "+title);

            //mCurrentDuration = settings.getLong("MusicDuration",0);
            /*mLastAlbumPath = settings.getString("LastAlbumPath","");
             mNextAlbumPath = settings.getString("NextAlbumPath","");
             mMusicQueueIndex = settings.getInt("MusicQueue",0);
             mLastQueueIndex = settings.getInt("MusicLastQueue",-1);
             mNextQueueIndex = settings.getInt("MusicNextQueue",-1);
             mCurrentSource = settings.getString("MusicSourceAlias","本地播放");
             mUserName = settings.getString("UserName","用户名");     //用户个性名称
             mUserLabel = settings.getString("UserLabel","Supreme"); //用户个性便笺
             mUserIconPath = settings.getString("IconPath","none");  //用户头像Url
             mCurrentBlurs = settings.getFloat("UserSetBlurs",13f);  //用户设置的模糊值
             mCurrentCycleTime = settings.getLong("UserCycleTime",36000);  //用户设置的唱片周期
             mCurrentVoiceMb = settings.getLong("UserVoiceMb",1000);  //用户设置的人声增强（mb）1000mb = 10db
             mCurrentFileMusicTitle = settings.getString("FileMusicTitle","");//默认为顺序播放
             mCurrentFileMusicArtist = settings.getString("FileMusicArtist","");//默认为顺序播放
             isNotificationMediaStyle = settings.getBoolean("NotificationStyle",true);
             if(!UsersGuidePermissionsUtil.checkCanDrawOverlays(this.getApplicationContext()))
                 isLrcViewShow = settings.getBoolean("LrcShow",false);*/return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, title.replace(" ".toRegex(), "_"))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumPath)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumPath)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path)
        }

    fun SaveLastMusicPlay(metadata: MediaMetadataCompat, position: Int) {
        val editor = get()!!.edit()
        editor.putString("MusicTitle", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        editor.putString("MusicArtist", metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
        editor.putString("MusicAlbum", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
        editor.putString("MusicPath", metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
        editor.putString("MusicAlbumPath", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
        editor.putInt("MusicPosition", position)
        editor.putLong("MusicDuration", metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
        /*editor.putString("LastAlbumPath", mLastAlbumPath);
        editor.putString("NextAlbumPath", mNextAlbumPath);
        editor.putInt("MusicQueue",mMusicQueueIndex);
        editor.putInt("MusicLastQueue",mLastQueueIndex);
        editor.putInt("MusicNextQueue",mNextQueueIndex);
        editor.putString("MusicSourceAlias",mCurrentSource);
        editor.putString("PlayMode",PLAYER_PLAY_MODE);
        editor.putString("UserName",mUserName);
        editor.putString("UserLabel",mUserLabel);
        editor.putString("IconPath",mUserIconPath);
        editor.putString("FileMusicTitle",mCurrentFileMusicTitle);
        editor.putString("FileMusicArtist",mCurrentFileMusicArtist);
        editor.putBoolean("LrcShow",isLrcViewShow);*/
        //Log.d(TAG, "SaveLastMusicPlay: mCurrentPosition= "+mCurrentPosition+"mMusicQueueIndex= "+mMusicQueueIndex);
        Log.d(TAG, "SaveLastMusicPlay: 是否保存成功 " + editor.commit())
    }

    fun onDestroy() {
        settings?.clear()
    }

    companion object {
        private const val TAG = "LastMetaManager"
    }

    init {
        settings = SoftReference(
            application.getSharedPreferences("UserLastMusicPlay", 0))
        if (settings.get() == null) Log.e(TAG, "LastMetaManager: SharedPreferences为空")
    }
}