package com.example.mediasessiontest.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

/**
 */
object ImmersiveStatusBarUtil {
    private const val TAG = "ImmersiveStatusBarUtil"

    /**
     * description 将状态栏设置为全透明
     * @param activity activity的上下文对象
     * @param IS_HIDE_NAVIGATION 是否隐藏底部导航栏(虚拟按键)
     * @author Alonso
     * time 2020/12/1 19:50
     */
    @TargetApi(19)
    fun transparentBar(activity: Activity, IS_HIDE_NAVIGATION: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //高于Android4.4版本
            //logJsonUtil.e("StatusBar","高于4.0");
            val window = activity.window
            //WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS 窗口标志：请求提供最小系统的半透明状态栏
            //WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION  窗口标志：请求提供最小系统的半透明导航栏
            //clearFlags()和addFlags()--->setFlags()
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) //清除标志
            if (IS_HIDE_NAVIGATION) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or  //请求系统暂时隐藏导航栏（navigation bar）
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //全屏
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //全屏
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) //请求提供最小半透明状态栏
            //给导航栏、状态栏设置透明颜色，以保护全局背景不被隔断、剪切、显示不美观
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            Toast.makeText(activity, "系统未提供透明信息栏方法", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 谷歌原生修改状态栏字体颜色
     * @param activity activity 上下文对象
     * @param dark 是否设置成黑色字体
     */
    fun setAndroidNativeLightStatusBar(activity: Activity, dark: Boolean) {
        val decor = activity.window.decorView
        if (dark) {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    /**
     * 隐藏界面打开的软键盘 */
    fun HideSoftInput(activity: Activity?) {
        if (activity == null) return
        Log.d(TAG, "HideSoftInput: ")
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive && activity.currentFocus != null) {
            if (activity.currentFocus!!.windowToken != null) {
                imm.hideSoftInputFromWindow(activity.currentFocus!!.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }

    fun ShowSoftInput(application: Application?, v: View?) {
        if (application == null || v == null) return
        val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }
}