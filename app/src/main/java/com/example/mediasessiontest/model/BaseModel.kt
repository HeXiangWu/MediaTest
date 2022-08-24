package com.example.mediasessiontest.model

import android.content.ContentResolver
import android.content.res.Resources
import com.example.mediasessiontest.bean.MusicBean
import android.support.v4.media.MediaMetadataCompat
import android.graphics.Bitmap
import java.lang.ref.WeakReference
import java.util.LinkedHashMap

/**
 * 作用: Model在程序中专门用于提供数据，
 * 不管是网络请求获得的数据，还是数据库获得的数据，统统写在Model里。
 * Model层独立性相当强，它只用来提供数据，而不管数据是用来做什么的。
 */
interface BaseModel {
    //获得本地音乐元数据，返回List<customBean>集合数据
    fun getLocalMusic(onMusicListener: OnMusicListener?, resolver: ContentResolver?)
    interface OnMusicListener {
        fun OnComplete(beans: List<MusicBean?>?)
    }

    //获得本地音乐元数据，返回List<MediaBrowserCompat.MediaItem>集合数据，适用于MediaSession媒体框架
    fun getLocalMusicMetadata(onMusicListener: OnMusicMetadataListener?, resolver: ContentResolver?)
    interface OnMusicMetadataListener {
        fun OnComplete(musicMaps: LinkedHashMap<String?, MediaMetadataCompat?>?)
    }

    //获得一首本地音乐的专辑图片，Bitmap
    fun getLocalMusicAlbum(onLoadPictureListener: OnLoadPictureListener?, path: String?, resources: Resources?)
    interface OnLoadPictureListener {
        fun OnComplete(bitmap: WeakReference<Bitmap?>?)
    }
}