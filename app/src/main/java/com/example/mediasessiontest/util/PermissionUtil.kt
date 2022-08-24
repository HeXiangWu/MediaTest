package com.example.mediasessiontest.util

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.example.mediasessiontest.util.PermissionUtil

/**
 * 作用: 获取读写权限
 */
object PermissionUtil {
    /*动态申请读、写权限*/
    const val REQUEST_PERMISSION_CODE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * 获取权限
     * @param activity activity活动的上下文对象
     * 注释：从Android 6.0开始 谷歌要求 获取权限 必须动态获取，已达到手机用户自主选择的目的
     */
    fun getStorage(activity: Activity?) {
        /*动态获取存储权限的函数*/
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) { //判断Android版本是否大于6.0 || 在API(26)以后规定必须要动态获取权限
            if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE)
            }
        }
    }

    /**
     * 检查是否未获取权限
     * @return true 未获取权限
     * false 已获取权限
     */
    fun IsPermissionNotObtained(activity: Activity?): Boolean {
        return ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED //判断是否已获取权限;
    }
}