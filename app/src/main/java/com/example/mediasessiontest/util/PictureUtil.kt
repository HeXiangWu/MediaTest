package com.example.mediasessiontest.util

import android.graphics.Bitmap
import com.example.mediasessiontest.util.PictureUtil
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.Allocation
import android.annotation.TargetApi
import android.app.Application
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Bitmap.CompressFormat
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.annotation.ColorInt
import android.graphics.drawable.LayerDrawable
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import android.text.TextUtils
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.renderscript.Element
import android.util.Log
import com.example.mediasessiontest.R
import jp.wasabeef.glide.transformations.internal.FastBlur
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.ref.WeakReference

/**
 * 作用: 图片（Bitmap）处理工具类
 * 1.Bitmap图片高斯模糊
 * 2.Bitmap图片裁剪
 * 3.图片文件保存到相册 。。。
 */
object PictureUtil {
    /**
     * 1.Bitmap高斯模糊
     * @param application 上下文对象
     * @param image   需要模糊的图片
     * @param blurRadius 设置渲染的模糊程度, 25f是最大模糊度,越大越模糊
     * @return 模糊处理后的Bitmap
     */
    @Deprecated("""从 Android 12 开始，RenderScript API 已被弃用。
      它们将继续正常运行，但我们预计设备和组件制造商会逐渐停止提供GPU硬件加速支持。
      https://developer.android.google.cn/guide/topics/renderscript/migrate
      建议Android 10以下使用，Android 10+则使用Vulkan
      """)
    fun blurBitmap(application: Application?, image: Bitmap, blurRadius: Float): Bitmap {
        val BITMAP_SCALE = 0.16f // 图片缩放比例
        // 计算图片缩小后的长宽
        val width = Math.round(image.width * BITMAP_SCALE)
        val height = Math.round(image.height * BITMAP_SCALE)

        // 将缩小后的图片做为预渲染的图片
        val inputBitmap = getItWeakReference(
            Bitmap.createScaledBitmap(image, width, height, false))!!
        // 创建一张渲染后的输出图片
        val outputBitmap = getItWeakReference(Bitmap.createBitmap(inputBitmap))!!
        // 创建RenderScript内核对象
        val rs = getItWeakReference(RenderScript.create(application))!!
        // 创建一个模糊效果的RenderScript的工具对象
        val blurScript = getItWeakReference(
            ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)))!!
        // 由于RenderScript并没有使用VM来分配内存,所以需要使用Allocation类来创建和分配内存空间
        // 创建Allocation对象的时候其实内存是空的,需要使用copyTo()将数据填充进去
        val tmpIn = getItWeakReference(Allocation.createFromBitmap(rs, inputBitmap))!!
        val tmpOut = getItWeakReference(Allocation.createFromBitmap(rs, outputBitmap))!!
        // 设置渲染的模糊程度, 25f是最大模糊度
        blurScript.setRadius(blurRadius)
        // 设置blurScript对象的输入内存
        blurScript.setInput(tmpIn)
        // 将输出数据保存到输出内存中
        blurScript.forEach(tmpOut)
        // 将数据填充到Allocation中
        tmpOut.copyTo(outputBitmap)
        //释放资源：RenderScript内核对象
        tmpIn.destroy()
        tmpOut.destroy()
        blurScript.destroy()
        rs.destroy()
        System.gc()
        return getItWeakReference(outputBitmap)!!
    }

    @TargetApi(Build.VERSION_CODES.S)
    fun blurBitmapEffect(application: Application?, image: Bitmap?, blurRadius: Float): RenderEffect {
        val effect = RenderEffect.createBlurEffect(blurRadius, blurRadius,
            RenderEffect.createBitmapEffect(image!!), Shader.TileMode.DECAL)
        return getItWeakReference(effect)!!
    }

    /**
     * 2.Bitmap图片裁剪,剪切专辑图片后得到Bitmap
     * @param bitmap 要从中截图的原始位图
     * @param x 宽与高的比值，确定横屏还是竖屏
     * @return 返回一个剪切好的Bitmap
     * @author 12453
     * date 2020/12/11 16:58
     */
    fun imageCropWithRect(bitmap: Bitmap?, x: Float): Bitmap? {
        if (bitmap == null) return null
        // 得到图片的宽，高    [诀窍: 竖屏取中间，横屏取上边]
        val isLandScreen = x > 1
        val w = bitmap.width
        val h = bitmap.height
        val retX: Int
        val retY: Int
        val nw: Int
        val nh: Int
        if (w > h) { //Log.d(TAG, "imageCropWithRect: W > H");
            nw = h / 2
            nh = h
            retX = (w - nw) / 2
            retY = 0
        } else { //Log.d(TAG, "imageCropWithRect: W <=Hm , w= "+w+", h= "+h+" , 比例 "+x);
            retX = if (isLandScreen) 0 else ((w - h * x) / 2).toInt()
            retY = 0
            nw = if (isLandScreen) w - retX else (h * x).toInt()
            nh = (if (isLandScreen) nw / x else h).toInt()
        }
        //Log.d(TAG, "imageCropWithRect: "+retX+", "+retY+", "+nw+", "+nh);
        return getItWeakReference(
            Bitmap.createBitmap(bitmap, retX, retY, nw, nh, null, false))
    }

    /**
     * 3.图片文件保存到相册,Api>=29时调用，保存图片至相册
     * @param application 上下文
     * @param bitmap 位图对象
     * @param displayName 文件名
     * @param mineType 图片格式说明
     * @param compressFormat 生成的图片格式
     * @author 12453
     * date 2020/12/31 15:46
     */
    fun addBitmapToAlbum(application: Application,  //上下文
                         bitmap: Bitmap,  //位图对象
                         displayName: String,  //文件名
                         mineType: String?,  //图片格式说明
                         compressFormat: CompressFormat?) { //生成的图片格式
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, mineType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/img")
        } else {
            values.put(MediaStore.MediaColumns.DATA, Environment.getExternalStorageDirectory().path
                + "/" + Environment.DIRECTORY_DCIM + displayName)
        }
        val resolver = application.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        values.clear()
        Log.d("图片保存路径", "" + uri)
        if (uri != null) {
            val outputStream: FileOutputStream?
            try {
                outputStream = resolver.openOutputStream(uri) as FileOutputStream?
                bitmap.compress(compressFormat, 100, outputStream) //png格式
                if (outputStream != null) {
                    outputStream.flush()
                    outputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * @param bm 原Bitmap位图对象
     * @param newWidth 新宽度
     * @param newHeight 新高度
     * @return 获得任意宽高的bitmap
     */
    fun zoomImg(bm: Bitmap?, newWidth: Int, newHeight: Int): Bitmap {
        // 获得图片的宽高
        val width = bm!!.width
        val height = bm.height
        // 计算缩放比例
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // 取得想要缩放的matrix参数
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)

        // 得到新的图片
        return getItWeakReference(
            Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true))!!
    }

    /** 创建一个圆形的图片drawable
     * @param application 单例持有的上下文对象
     * @param bitmap 位图对象，通过各种途径 获得
     * @param bitmapWidth 需要创建多大的Bitmap，设置其宽度
     * @param bitmapHeight 需要创建多大的Bitmap，设置其高度
     * @return 返回一个圆形的RoundedBitmapDrawable
     */
    fun createCircleDrawable(application: Application?, bitmap: Bitmap?,
                             bitmapWidth: Int, bitmapHeight: Int): RoundedBitmapDrawable? {
        var bitmap = bitmap
        if (application == null || bitmap == null) return null
        bitmap = zoomImg(bitmap, bitmapWidth, bitmapHeight)
        val drawable = RoundedBitmapDrawableFactory.create(application.resources, bitmap)
        drawable.isCircular = true
        return drawable
    }

    /** 创建一个圆形、纯色包边的图层叠加drawable专辑唱片 - LayerDrawable
     * @param application 单例持有的上下文对象
     * @param bitmap 位图对象，通过各种途径 获得
     * @param bitmapSize 需要创建多大的Bitmap，设置其宽高大小
     * @param stokeWidth drawable包边宽度
     * @param color int型的颜色，使用源文件values/color.xml 或者 [Color.argb] 获得
     * @return 返回一个圆形的RoundedBitmapDrawable
     * <a href>https://blog.csdn.net/zhangphil/article/details/52045404</a>
     */
    fun createCircleDrawable(application: Application?, bitmap: Bitmap?,
                             viewSize: Float, bitmapSize: Int,
                             stokeWidth: Int, @ColorInt color: Int): LayerDrawable? {
        var bitmap = bitmap
        if (application == null) return null
        val resources = application.resources
        if (bitmap == null) bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_fate)
        val layers = arrayOfNulls<Drawable>(3)
        //最外部的半透明边线
        val ovalShape0 = OvalShape()
        val drawable0 = ShapeDrawable(ovalShape0)
        drawable0.paint.color = color
        drawable0.paint.style = Paint.Style.FILL
        drawable0.paint.isAntiAlias = true
        layers[0] = drawable0
        val record = BitmapFactory.decodeResource(resources, R.drawable.iv_record_128)
        val drawable1 = RoundedBitmapDrawableFactory.create(resources, record)
        drawable1.isCircular = true
        layers[1] = drawable1
        bitmap = zoomImg(bitmap, bitmapSize, bitmapSize)
        val drawable2 = RoundedBitmapDrawableFactory.create(resources, bitmap)
        drawable2.isCircular = true
        layers[2] = drawable2
        val recordSize = (viewSize * 0.21).toInt()
        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setLayerInset(0, 0, 0, 0, 0)
        layerDrawable.setLayerInset(1, stokeWidth, stokeWidth, stokeWidth, stokeWidth)
        layerDrawable.setLayerInset(2, recordSize, recordSize, recordSize, recordSize)
        return WeakReference(layerDrawable).get()
    }

    fun createCircleDrawableBig(application: Application?, target: Bitmap?, size: Int,
                                viewSize: Int): LayerDrawable? {
        var target = target
        if (application == null) return null
        val resources = application.resources
        if (target == null) target = getItWeakReference(BitmapFactory.decodeResource(resources, R.drawable.icon_fate))
        val layers = arrayOfNulls<Drawable>(2)
        val record = getItWeakReference(
            BitmapFactory.decodeResource(resources, R.drawable.iv_record))!!
        val drawable1 = getItWeakReference(RoundedBitmapDrawableFactory
            .create(resources, zoomImg(record, 400, 400)))!!
        drawable1.isCircular = true
        layers[0] = getItWeakReference(drawable1)
        val drawable2 = getItWeakReference(RoundedBitmapDrawableFactory
            .create(resources, zoomImg(target, 200, 200)))!!
        //drawable2.setAntiAlias(true);
        drawable2.isCircular = true
        layers[1] = getItWeakReference(drawable2)
        val layerDrawable = getItWeakReference(LayerDrawable(layers))!!
        val insetWidth = (viewSize * 0.084).toInt()
        //针对每一个图层进行填充，使得各个圆环之间相互有间隔，否则就重合成一个了。
        //Log.d(TAG, "setImageBitmap: "+getMeasuredWidth());
        layerDrawable.setLayerInset(0, 0, 0, 0, 0)
        layerDrawable.setLayerInset(1, insetWidth, insetWidth, insetWidth, insetWidth)
        return WeakReference(layerDrawable).get()
    }

    /** 创建一个圆形的纯色drawable
     * @param context 单例持有的上下文对象
     * @param color int型的颜色，使用源文件values/color.xml 或者 [Color.argb] 获得
     * @param bitmapWidth 需要创建多大的Bitmap，设置其宽度
     * @param bitmapHeight 需要创建多大的Bitmap，设置其高度
     * @return 返回一个圆形、纯色的RoundedBitmapDrawable
     * <a href>https://blog.csdn.net/u010054982/article/details/52487599?utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-1.control&dist_request_id=1619603308189_75864&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-1.control</a>
     */
    fun createColorDrawable(context: Context?, @ColorInt color: Int,
                            bitmapWidth: Int, bitmapHeight: Int): RoundedBitmapDrawable? {
        var context = context
        var color = color
        if (context == null) return null
        context = context.applicationContext
        if (color == 0) {
            color = Color.parseColor("#EEEEEE")
        }
        val colorBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        colorBitmap.eraseColor(color)
        val drawable = RoundedBitmapDrawableFactory.create(context.resources, colorBitmap)
        drawable.isCircular = true
        return WeakReference(drawable).get()
    }

    fun createBlurDrawable(application: Application?, width: Float, height: Int,
                           blur: Float, targetBitmap: Bitmap?): LayerDrawable? {
        var targetBitmap = targetBitmap
        if (application == null || width <= 0 || height <= 0) return null
        val resources = application.resources
        if (targetBitmap == null) targetBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_fate)
        val layers = arrayOfNulls<Drawable>(2)
        var screenshot = imageCropWithRect(targetBitmap, width / height)
        screenshot = zoomImg(screenshot, 500, 500)
        screenshot = FastBlur.blur(screenshot, 45, true)
        layers[0] = BitmapDrawable(resources, screenshot)
        val maskBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        maskBitmap.eraseColor(Color.argb(100, 0, 0, 0))
        layers[1] = BitmapDrawable(resources, maskBitmap)
        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setLayerInset(0, 0, 0, 0, 0)
        layerDrawable.setLayerInset(1, 0, 0, 0, 0)
        //return new BitmapDrawable(context.getResources(),screenshot);
        return WeakReference(layerDrawable).get()
    }

    /*返回一个带白色圆边的头像Icon*/
    fun createUserIconDrawable(application: Application?, target: Bitmap?, size: Int,
                               viewSize: Int): LayerDrawable? {
        var target = target
        if (application == null) return null
        val resources = application.resources
        if (target == null) target = getItWeakReference(BitmapFactory.decodeResource(resources, R.drawable.icon_fate))
        val layers = arrayOfNulls<Drawable>(2)
        val colorBg = getItWeakReference<RoundedBitmapDrawable?>(
            createColorDrawable(application, Color.parseColor("#28EEEEEE"),
                120, 120))
        layers[0] = getItWeakReference<RoundedBitmapDrawable?>(colorBg)
        val drawable2 = getItWeakReference(RoundedBitmapDrawableFactory
            .create(resources, zoomImg(target, 200, 200)))!!
        //drawable2.setAntiAlias(true);
        drawable2.isCircular = true
        layers[1] = getItWeakReference(drawable2)
        val layerDrawable = getItWeakReference(LayerDrawable(layers))!!
        val insetWidth = (viewSize * 0.05).toInt()
        //针对每一个图层进行填充，使得各个圆环之间相互有间隔，否则就重合成一个了。
        //Log.d(TAG, "setImageBitmap: "+getMeasuredWidth());
        layerDrawable.setLayerInset(0, 0, 0, 0, 0)
        layerDrawable.setLayerInset(1, insetWidth, insetWidth, insetWidth, insetWidth)
        return WeakReference(layerDrawable).get()
    }

    fun getResIdBitmap(@DrawableRes resId: Int, size: Int, resources: Resources?, roundCorner: Int): Bitmap? {
        if (resources == null || size <= 0 || size > 800 || roundCorner < 0 || roundCorner > 360) return null
        val drawable = ResourcesCompat.getDrawable(resources, resId, null)
        if (drawable != null) {
            var bitmap = zoomImg(drawableToBitmap(drawable), size, size)
            if (roundCorner > 0) {
                val drawable1 = RoundedBitmapDrawableFactory.create(resources, bitmap)
                drawable1.cornerRadius = roundCorner.toFloat()
                bitmap = drawableToBitmap(drawable1)
            }
            return bitmap
        }
        return null
    }

    fun getResIdBitmap(bitmap: Bitmap?, size: Int, resources: Resources?, roundCorner: Int): Bitmap? {
        var bitmap = bitmap
        if (resources == null || size <= 0 || size > 800 || roundCorner < 0 || roundCorner > 360) return null
        bitmap = zoomImg(bitmap, size, size)
        if (roundCorner > 0) {
            val drawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
            drawable.cornerRadius = roundCorner.toFloat()
            bitmap = drawableToBitmap(drawable)
        }
        return bitmap
    }

    /**
     * 将Bitmap转成本地图片
     * @param bitmap 已有网络图片
     * @param targetPath 保存的本地路径
     */
    fun SaveBitmapCache(bitmap: Bitmap?, targetPath: String?) {
        if (bitmap == null) return
        if (TextUtils.isEmpty(targetPath)) return
        try {
            val file = File(targetPath)
            val out = FileOutputStream(file)
            bitmap.compress(CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            Log.e("PictureUtil", "Bitmap保存失败 $e")
        }
    }

    /**
     * Drawable转换成一个Bitmap
     *
     * @param drawable drawable对象
     * @return bitmap
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    fun <T> getItWeakReference(obj: T?): T? {
        return if (obj == null) null else WeakReference(obj).get()
    }
}