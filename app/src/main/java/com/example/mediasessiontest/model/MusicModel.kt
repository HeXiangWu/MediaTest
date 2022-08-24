package com.example.mediasessiontest.model

import com.example.mediasessiontest.model.BaseModel.OnMusicListener
import android.content.ContentResolver
import com.example.mediasessiontest.model.BaseModel.OnMusicMetadataListener
import com.example.mediasessiontest.model.BaseModel.OnLoadPictureListener
import android.graphics.Bitmap
import com.example.mediasessiontest.R
import android.annotation.SuppressLint
import android.content.res.Resources
import android.database.Cursor
import com.example.mediasessiontest.bean.MusicBean
import com.example.mediasessiontest.model.MusicModel
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.mediasessiontest.util.PictureUtil
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * 作用:
 */
class MusicModel : BaseModel {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun getLocalMusic(onMusicListener: OnMusicListener?, resolver: ContentResolver?) {
        onMusicListener!!.OnComplete(getLocalMusic(resolver))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getLocalMusicMetadata(onMusicListener: OnMusicMetadataListener?, resolver: ContentResolver?) {
        onMusicListener!!.OnComplete(getLocalMusicMetadata(resolver))
    }

    override fun getLocalMusicAlbum(onLoadPictureListener: OnLoadPictureListener?, path: String?, resource: Resources?) {
        var bitmap = getAlbumBitmap(path)
        bitmap = bitmap ?: PictureUtil.getResIdBitmap(R.drawable.icon_fate, 500, resource, 0)
        onLoadPictureListener!!.OnComplete(WeakReference(bitmap))
    }

    /**
     * 描述 获取本地音乐列表
     * @param resolver 内容访问器，这个类提供对内容模型的应用程序访问。
     * @return List<MusicBean>类型的本地音乐列表集合
    </MusicBean> */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("Range")
    private fun getLocalMusic(resolver: ContentResolver?): List<MusicBean?>? {
        Log.e(TAG, "getLocalMusic: " + (resolver == null))
        if (resolver == null) return null
        val beans: MutableList<MusicBean?> = ArrayList()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = resolver.query(uri, null, null, null)
        var id = 0
        while (cursor != null && cursor.moveToNext()) {
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
            if (duration < 90000) continue
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
            val path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
            id++
            val sid = id.toString()
            val bean = MusicBean(sid,
                getSpanishStr(title)!!,
                getSpanishStr(artist)!!,
                getSpanishStr(album)!!,
                path, path, duration)
            beans.add(bean)
            //Log.d(TAG, "getLocalMusic: "+getUtf8(title));
        }
        if (cursor != null) {
            cursor.close()
            if (!cursor.isClosed) cursor.close()
        }
        Log.d(TAG, "getLocalMusic: " + beans.size)
        return beans
    }

    /**
     * @param resolver 内容访问器，这个类提供对内容模型的应用程序访问。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("Range")
    private fun getLocalMusicMetadata(resolver: ContentResolver?): LinkedHashMap<String?, MediaMetadataCompat?>? {
        //Log.e(TAG, "getLocalMusicMetadata: "+(resolver == null));
        if (resolver == null) return null
        val result = LinkedHashMap<String?, MediaMetadataCompat?>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, null, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        while (cursor != null && cursor.moveToNext()) {
            val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
            if (duration < 90000) continue
            var title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
            var artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
            var album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
            val mediaId = title!!.replace(" ".toRegex(), "_")
            //西班牙语重音字母替换
            title = getSpanishStr(title)
            artist = getSpanishStr(artist)!!.replace("&".toRegex(), "/")
            album = getSpanishStr(album)
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "")
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, path)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, path)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path)
                .build()
            result[mediaId] = metadata
            //Log.d(TAG, "getLocalMusicMetadata: "+artist);
        }
        if (cursor != null) {
            cursor.close()
            if (!cursor.isClosed) cursor.close()
        }
        //Log.d(TAG, "getLocalMusicMetadata: "+result.size());
        return result
    }

    /**
     * description 返回一个本地音乐文件的专辑bitmap图片
     * @param Path 给定当前点击item音乐的外部存储路径，非content
     */
    private fun getAlbumBitmap(Path: String?): Bitmap? {
        if (Path!!.isEmpty()) return null //返回默认的专辑封面
        if (!FileExists(Path)) return null //找不到文件返回空
        Log.d(TAG, "getAlbumBitmap: $Path")
        return if (!Path.contains(".mp3")) {
            val bitmap: Bitmap
            var bis: BufferedInputStream? = null
            try {
                bis = BufferedInputStream(FileInputStream(Path))
                bitmap = BitmapFactory.decodeStream(bis)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("AllSongSheetModel", "getAlbumBitmap: 本地图片转Bitmap失败")
                return null
            } finally {
                if (bis != null) {
                    try {
                        bis.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.d("AllSongSheetModel", "getAlbumBitmap: 输出流关闭异常")
                    }
                }
            }
            //Log.d("加载本地图片", "getAlbumBitmap: ");
            bitmap
        } else {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(Path)
            val picture = metadataRetriever.embeddedPicture

            /*每次拿到专辑图片后，关闭MediaMetadataRetriever对象，等待GC器回收内存
             *以便下一次再重新引用（new），避免内存泄漏*/metadataRetriever.release() //SDK > 26 才有close，且close与release是一样的
            //返回默认的专辑封面
            if (picture == null) null else BitmapFactory.decodeByteArray(picture, 0, picture.size)
        }
    }

    companion object {
        private const val TAG = "MusicModel"

        /**
         * 将西班牙语重音乱码替换为UTF-8
         */
        private fun getSpanishStr(str: String?): String? {
            return if (str == null || TextUtils.isEmpty(str)) str else str.replace("¨¢".toRegex(), "á")
                .replace("¨¦".toRegex(), "é")
                .replace("¨Ş".toRegex(), "í")
                .replace("¨ª".toRegex(), "í")
                .replace("¨®".toRegex(), "ó")
                .replace("¨²".toRegex(), "ú")
        }

        fun FileExists(targetFileAbsPath: String?): Boolean {
            try {
                val f = File(targetFileAbsPath)
                if (!f.exists()) return false
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }
    }
}