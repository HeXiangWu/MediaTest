package com.example.mediasessiontest.custom

import android.animation.Animator
import android.app.Activity
import com.example.mediasessiontest.custom.SlideView.MySlideView
import android.widget.FrameLayout
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.example.mediasessiontest.custom.SlideView
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.mediasessiontest.R
import java.util.*

/**
 * 作用: 跟随手指滑动，而关闭的透明样式的Activity | ViewGroup
 */
class SlideView(activity: Activity, slideDirection: String) {
    private var mSlideView: MySlideView?
    fun setNoScrollEvent(noScrollEvent: Boolean) {
        if (mSlideView != null) mSlideView!!.setNoScrollEvent(noScrollEvent)
    }

    fun onDestroy() {
        if (mSlideView != null) {
            mSlideView!!.release()
            mSlideView = null
        }
    }

    /**
     * 核心的View 提供activity滑动是否关闭，手势监听， 触摸事件是否消费
     */
    inner class MySlideView : FrameLayout {
        constructor(context: Context) : super(context) {
            touchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

        private val TAG = MySlideView::class.java.simpleName
        private var activity // 绑定的Activity
            : Activity? = null

        //private ShowCoverActivity mShowCoverActivity;
        private var decorView: ViewGroup? = null
        private var contentView // activity的ContentView
            : View? = null
        private var intercept_X = 0f // onInterceptTouchEvent刚触摸时的X坐标
        private var intercept_Y = 0f
        private val contentViewY_last = 0f
        private val moveY_last // onInterceptTouchEvent手指刚触摸时的y坐标
            = 0f
        private var touchSlop = 0
        private var closedArea = 3 // 产生滑动的最小值,关闭区域默认3，当滑动到至少屏幕高度三分之一后松手关闭activity
        private var   /*mPhoneWidth,*/mPhoneHeight = 0
        private var direction //滑动方向
            : String? = null
        private var isVerticalScroll = false
        private var NoScrollEvent = true //初始不可滑动关闭
        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
            NoScrollEvent = true
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            //if (mShowCoverActivity != null) mShowCoverActivity = null;
            release()
        }

        fun release() {
            if (activity != null) activity = null
            if (decorView != null) decorView = null
            if (contentView != null) contentView = null
            if (direction != null) direction = null
        }

        fun setNoScrollEvent(noScrollEvent: Boolean) {
            NoScrollEvent = noScrollEvent
        }

        /**
         * 绑定Activity
         * @param activity 需要滑动关闭的Activity对象
         * @param direction activity滑动关闭的方向
         */
        fun setActivity(activity: Activity?, direction: String) {
            //if (activity instanceof ShowCoverActivity) this.mShowCoverActivity = (ShowCoverActivity) activity;
            this.activity = activity
            this.direction = direction
            isVerticalScroll = direction == SLIDE_DIRECTION_UP || direction == SLIDE_DIRECTION_DOWN
            //Log.d(TAG, "setActivity: "+isVerticalScroll);
            initCoverView()
            //Log.d(TAG, "showCoverActivity 是否等于空"+(mShowCoverActivity == null));
        }

        /**
         * 将contentView从DecorView中移除，并添加到CoverView中，最后再将CoverView添加到DecorView中
         */
        private fun initCoverView() {
            decorView = activity!!.window.decorView as ViewGroup
            //if (mShowCoverActivity != null) decorView.setBackgroundColor(Color.parseColor("#000000"));
            contentView = decorView!!.findViewById(android.R.id.content)
            val contentParent = contentView?.getParent() as ViewGroup
            contentParent.removeView(contentView)
            addView(contentView)
            contentView?.setBackgroundColor(Color.TRANSPARENT)
            contentParent.addView(this)
            //
            val dm = DisplayMetrics()
            activity!!.window.windowManager.defaultDisplay.getMetrics(dm)
            //mPhoneWidth = dm.widthPixels;
            mPhoneHeight = dm.heightPixels
            if (dm.heightPixels >= 1600) closedArea = 4
            //Log.d(TAG, "initCoverView: "+/*mPhoneWidth+*/" "+mPhoneHeight+" "+closedArea);
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            return shouldInterceptEvent(ev)
        }

        /**
         * 判断是否应该拦截事件。
         * 如果水平方向的偏移量(不取绝对值) > 垂直方向的偏移量(取绝对值)，并且水平方向的偏移量大于最小滑动距离，我们将拦截事件。
         * 【实际过程中，我们发现touchSlope还是偏小，所以取了其3倍的数值作为最小滑动距离】
         * @param event 事件对象
         * @return true表示拦截，false反之
         */
        private fun shouldInterceptEvent(event: MotionEvent): Boolean {
            var shouldInterceptEvent = false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    intercept_X = event.x
                    intercept_Y = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (NoScrollEvent) return false
                    Log.d(TAG, "shouldInterceptEvent: ")
                    val offsetY = Math.abs(event.y - intercept_Y)
                    val offsetX = Math.abs(event.x - intercept_X)
                    shouldInterceptEvent = if ((if (isVerticalScroll) offsetY else offsetX) < touchSlop || (if (isVerticalScroll) offsetX > offsetY else offsetY > offsetX)) {
                        false
                    } else if ((if (isVerticalScroll) offsetY else offsetX) >= touchSlop) {
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    shouldInterceptEvent = false
                    NoScrollEvent = true
                }
                else -> {}
            }
            Log.d(TAG, "shouldInterceptEvent: $shouldInterceptEvent")
            return shouldInterceptEvent
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            processTouchEvent(event)
            return true
        }

        /**
         * 对onTouchEvent事件进行处理
         * @param event 事件对象
         */
        private fun processTouchEvent(event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {}
                MotionEvent.ACTION_MOVE -> {
                    val offsetX = event.x - intercept_X
                    val offsetY = event.y - intercept_Y
                    when (direction) {
                        SLIDE_DIRECTION_RIGHT ->                             //如果是向右滑动，我们动态改变contentView的偏移值
                            if (offsetX > 0) {
                                contentView!!.translationX = offsetX
                            }
                        SLIDE_DIRECTION_LEFT -> if (offsetX < 0) {
                            contentView!!.translationX = offsetX
                        }
                        SLIDE_DIRECTION_UP ->                             //如果是向下滑动，我们动态改变contentView的偏移值
                            if (offsetY < 0) {
                                contentView!!.translationY = offsetY
                            }
                        SLIDE_DIRECTION_DOWN ->                             //如果是向下滑动，我们动态改变contentView的偏移值
                            if (offsetY > 0) {
                                contentView!!.translationY = offsetY
                            }
                    }
                }
                MotionEvent.ACTION_UP -> when (direction) {
                    SLIDE_DIRECTION_RIGHT -> if (contentView!!.translationX >= contentView!!.measuredWidth.toFloat() / closedArea) {
                        collapse()
                    } else open()
                    SLIDE_DIRECTION_LEFT -> if (contentView!!.translationX <= 0 - contentView!!.measuredWidth.toFloat() / closedArea) {
                        collapse()
                    } else open()
                    SLIDE_DIRECTION_UP -> if (contentView!!.translationY <= 0 - contentView!!.measuredHeight.toFloat() / closedArea) {
                        collapse()
                    } else open()
                    SLIDE_DIRECTION_DOWN -> if (contentView!!.translationY >= contentView!!.measuredHeight.toFloat() / closedArea) {
                        collapse()
                    } else open()
                }
                else -> {}
            }
        }

        /**
         * 展开Activity
         */
        private fun open() {
            contentView!!.clearAnimation()
            var anim: ObjectAnimator? = null
            when (direction) {
                SLIDE_DIRECTION_RIGHT, SLIDE_DIRECTION_LEFT -> anim = ObjectAnimator.ofFloat(contentView, TRANSLATION_X, 0f)
                SLIDE_DIRECTION_UP, SLIDE_DIRECTION_DOWN -> anim = ObjectAnimator.ofFloat(contentView, TRANSLATION_Y, 0f)
            }
            anim?.start()
        }

        /**
         * 折叠Activity(finish掉)
         */
        private fun collapse() {
            contentView!!.clearAnimation()
            var anim: ObjectAnimator? = null
            when (direction) {
                SLIDE_DIRECTION_RIGHT -> anim = ObjectAnimator.ofFloat(contentView, TRANSLATION_X, contentView!!.measuredWidth.toFloat())
                SLIDE_DIRECTION_LEFT -> anim = ObjectAnimator.ofFloat(contentView, TRANSLATION_X, -contentView!!.measuredWidth.toFloat())
                SLIDE_DIRECTION_UP -> anim = ObjectAnimator.ofFloat(contentView, TRANSLATION_Y, -contentView!!.measuredHeight.toFloat())
                SLIDE_DIRECTION_DOWN -> anim = ObjectAnimator.ofFloat(contentView, TRANSLATION_Y, contentView!!.measuredHeight.toFloat())
            }
            if (anim != null) {
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (activity != null) {
                            activity!!.finish()
                            activity!!.overridePendingTransition(0, R.style.noAnimation)
                        }
                        //Log.d(TAG, "onAnimationEnd: ");
                    }
                })
                anim.duration = 100 //设置滑动动画的演示时长
                anim.start()
            }
        }

        /**
         * 根据屏幕Y轴移动距离 设置背景黑色的透明度
         * @param  y contentView 上下滑动时与屏幕顶部的绝对值
         */
        //当透明Activity 是{@link ShowCoverActivity}时调用
        private fun setDecorViewBgAlpha(y: Float) {
            if (decorView == null || mPhoneHeight <= 0 || y > mPhoneHeight) return
            val ySum = (mPhoneHeight shr 2).toFloat()
            var AlphaMultiple = ((ySum - y) / ySum * 256).toInt()
            if (AlphaMultiple < 76) AlphaMultiple = 76 // 256 * 0.3 = 76.8
            /* //Log.d(TAG, "setDimAmount: "+Integer.toHexString(AlphaMultiple).toUpperCase());
            * Integer.toHexString(int) : 2进制转16进制(小写)
            * String.totoUpperCase() ：有小写字母就转化成大写字母*/if (AlphaMultiple < 256) decorView!!.setBackgroundColor(Color.parseColor("#" + Integer.toHexString(AlphaMultiple).uppercase(Locale.getDefault()) + "000000")) else decorView!!.setBackgroundColor(Color.parseColor("#000000"))

            /*if (mShowCoverActivity != null){
                float multiple = (ySum - y) / ySum;
                multiple = Float.parseFloat(String.format(Locale.CHINA,"%.2f",multiple));
                mShowCoverActivity.dynamicCover(multiple);
            }*/
        }
    }

    companion object {
        private const val SLIDE_DIRECTION_LEFT = "SlideView_left"
        private const val SLIDE_DIRECTION_RIGHT = "SlideView_right"
        private const val SLIDE_DIRECTION_UP = "SlideView_up"
        const val SLIDE_DIRECTION_DOWN = "SlideView_down"
    }

    init {
        mSlideView = MySlideView(activity)
        mSlideView!!.setActivity(activity, slideDirection)
    }
}