package com.example.mediasessiontest.viewmodel

import android.app.Application
import com.example.mediasessiontest.util.PictureUtil.createColorDrawable
import com.example.mediasessiontest.util.PictureUtil.createCircleDrawable
import com.example.mediasessiontest.util.PictureUtil.createCircleDrawableBig
import com.example.mediasessiontest.util.PictureUtil.createBlurDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.databinding.PropertyChangeRegistry
import android.support.v4.media.session.MediaControllerCompat
import androidx.databinding.Observable.OnPropertyChangedCallback
import android.util.TypedValue
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.support.v4.media.session.PlaybackStateCompat
import androidx.databinding.Observable
import com.example.mediasessiontest.service.manager.MediaPlayerManager
import com.example.mediasessiontest.R
import java.lang.ref.SoftReference

/**
 * 参考 ： https://developer.android.google.cn/topic/libraries/data-binding/architecture?hl=zh_cn
 * 作用 : A ViewModel that is also an Observable,
 * to be used with the Data Binding Library.
 * 生命周期 ：[BaseViewModel]存在的时间范围是从您首次请求 [ViewModel] 直到 activity 完成并销毁。
 * 如果是Fragment则在Fragment从Activity分离后销毁
 * [BaseViewModel]销毁时执行继承自父类[ViewModel.onCleared]生命周期方法完成销毁，
 * 所以在里面完成您ViewModel资源的释放。
 * 注 ：[ViewModel]不可以持有View、Lifecycle、Activity引用 !!!!!!
 * 而且不能够包含任何包含前面内容的类。因为这样很有可能会造成内存泄漏 !!!!!! adapter最好也不要有
 * 如果 [ViewModel] 需要 Application 上下文（例如，为了查找系统服务），
 * 它可以扩展 [AndroidViewModel] 类并设置用于接收 Application 的构造函数，
 * 因为 Application 类会扩展 Context。
 */
abstract class BaseViewModel(application: Application?) : AndroidViewModel(application!!), Observable {
    private var callbacks: PropertyChangeRegistry? = PropertyChangeRegistry()
    @JvmField
    protected var mMediaControllerCompat: MediaControllerCompat? = null
    fun setMediaControllerCompat(mediaControllerCompat: MediaControllerCompat?) {
        mMediaControllerCompat = mediaControllerCompat
    }

    override fun addOnPropertyChangedCallback(
        callback: OnPropertyChangedCallback) {
        callbacks!!.add(callback)
    }

    override fun removeOnPropertyChangedCallback(
        callback: OnPropertyChangedCallback) {
        callbacks!!.remove(callback)
    }

    /**
     * Notifies observers that all properties of this instance have changed.
     */
    fun notifyChange() {
        callbacks!!.notifyCallbacks(this, 0, null)
    }

    /**
     * Notifies observers that a specific property has changed. The getter for the
     * property that changes should be marked with the @Bindable annotation to
     * generate a field in the BR class to be used as the fieldId parameter.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    fun notifyPropertyChanged(fieldId: Int) {
        callbacks!!.notifyCallbacks(this, fieldId, null)
    }

    /** ViewModel类的生命周期方法。
     * 当不再使用此ViewModel并将其销毁时，将调用此方法。
     * 当ViewModel观察到一些数据并且需要清除此订阅以防止此ViewModel泄漏时，
     * 此选项非常有用。 */
    override fun onCleared() {
        super.onCleared()
        if (callbacks != null) {
            callbacks!!.clear()
            callbacks = null
        }
        if (mMediaControllerCompat != null) mMediaControllerCompat = null
    }

    protected fun dpToPx(dp: Int, application: Application): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), application.resources.displayMetrics).toInt()
    }

    /*
    * 已用Shape资源文件代替
    * */
    @Deprecated("")
    protected fun getRecordBg(application: Application?): RoundedBitmapDrawable? {
        return createColorDrawable(application,
            Color.argb(18, 255, 255, 255), 400, 400)
    }

    //绘制小唱片
    protected fun getRecord(bitmap: Bitmap?, application: Application): LayerDrawable? {
        return createCircleDrawable(application, bitmap,
            dpToPx(42, application).toFloat(), 100, 1,
            Color.argb(32, 255, 255, 255))
    }

    //绘制SongLrc页面的大唱片
    protected fun getRecordBig(bitmap: Bitmap?, application: Application?, size: Int): LayerDrawable? {
        return createCircleDrawableBig(application, bitmap, 400, size)
    }

    //绘制高斯模糊背景
    protected fun getBlurDrawable(bitmap: Bitmap?, application: Application?): LayerDrawable? {
        return createBlurDrawable(application, 1080f, 1920, 20f, bitmap)
    }

    protected fun <T> getSoftReference(obj: T): T? {
        return SoftReference(obj).get()
    }

    private val customAction: PlaybackStateCompat.CustomAction
        private get() = PlaybackStateCompat.CustomAction.Builder(
            MediaPlayerManager.CUSTOM_ACTION_PLAYBACK_MODE_CHANGE,
            "playback_mode_change",
            R.drawable.iv_playback_mode_order).build()
}