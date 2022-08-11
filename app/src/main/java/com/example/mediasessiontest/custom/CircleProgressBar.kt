package com.example.mediasessiontest.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import com.example.mediasessiontest.R

/**
 * 参考 :  https://cloud.tencent.com/developer/article/1153093
 * 作用 : 显示效果为圆形进度条，确定的实时进度跳，1000ms更新3次
 * 仿网易云音乐app主页的圆形进度条，为用户显示当前的音乐播放进度
 */

class CircleProgressBar : View {
    //画笔
    private var mPaint: Paint? = null

    //圆形图片的半径
    private var mRadius = 0f

    //圆形进度条颜色
    private var fullColor = 0x20000000
    private var progressColor = -0x1000000

    //圆形进度条宽度
    private var strokeWidth = 3.6f
    private var distanceBoundary = 8f

    //圆形进度：音乐播放进度
    private var progress = 0
    private val min = 0
    private var max = 0
    private var progressOfAll = 0f
    private var mRectF: RectF? = null

    constructor(context: Context?) : super(context) {
        Log.d(TAG, "CircleProgressBar: 1")
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        Log.d(TAG, "CircleProgressBar: 2")
        if (attrs != null) {
            Log.w(TAG, "CircleProgressBar: 读取布局文件参数")
            val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressBar)
            fullColor = ta.getInt(R.styleable.CircleProgressBar_color_bg, 0x20000000)
            progressColor = ta.getInt(R.styleable.CircleProgressBar_color_progress, -0x1000000)
            strokeWidth = ta.getFloat(R.styleable.CircleProgressBar_width_stroke, 3.6f)
            distanceBoundary = ta.getFloat(R.styleable.CircleProgressBar_distance_boundary, 8f)
            progress = ta.getInt(R.styleable.CircleProgressBar_progress, 0)
            max = ta.getInt(R.styleable.CircleProgressBar_max, 0)
            ta.recycle()
        }
        if (mPaint == null) initPaint()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        Log.d(TAG, "CircleProgressBar: 3")
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = measuredHeight
        //Log.d(TAG, "onMeasure: Width = "+width+",Height"+height);
        //由于是圆形，宽高应保持一致
        val size = Math.min(width, height)
        //Log.d(TAG, "onMeasure: size = "+size);
        mRadius = size / 2f
        setMeasuredDimension(size, size) //测量高度和宽度
        mRectF = RectF(strokeWidth + distanceBoundary,
            strokeWidth + distanceBoundary,
            width - strokeWidth - distanceBoundary,
            height - strokeWidth - distanceBoundary)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //Log.d(TAG, "onDraw: "+mRadius+", 控件宽度 = "+getWidth()+", 控件高度 = "+getHeight());
        //画圆框进度条底框,radius = radius - 8 - strokeWidth
        canvas.drawCircle(mRadius, mRadius, mRadius - strokeWidth - distanceBoundary, mPaint!!)
        mPaint!!.color = progressColor
        mRectF?.let { canvas.drawArc(it, 270f, progressOfAll, false, mPaint!!) }
        mPaint!!.color = fullColor
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //Log.d(TAG, "onDetachedFromWindow: ");释放资源
        if (mPaint != null) {
            mPaint!!.reset()
            mPaint = null
        }
        if (mRectF != null) mRectF = null
    }

    private fun initPaint() {
        mPaint = Paint()
        mPaint!!.color = fullColor //设置画笔颜色
        mPaint!!.isAntiAlias = true
        mPaint!!.style = Paint.Style.STROKE //设置画笔模式为描边
        mPaint!!.strokeWidth = strokeWidth //设置画笔宽度
    }

    /**
     * 更新界面
     * @param progress 音乐播放的进度
     * 建议 1秒 | 1000毫秒 执行三次
     */
    fun setProgress(progress: Int) {
        if (progress < 0 || max == 0) {
            Log.e(TAG, "设置进度失败！$progress")
            return
        }
        progressOfAll = progress.toFloat() / max * 360f
        //在事件循环的后续循环中导致失效。用此选项可使非UI线程中的视图无效
        //仅当此视图附着到窗口时,此方法可以从UI线程外部调用。</p
        //Log.d(TAG, "setProgress: 圆形进度为 "+progress);
        postInvalidate() //绘制刷新
    }

    fun setMax(max: Int) {
        this.max = max
    }

    fun setProgressColor(@ColorInt progressColor: Int) {
        this.progressColor = progressColor
    }

    companion object {
        private const val TAG = "CircleProgressBar"
    }
}