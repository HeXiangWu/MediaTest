package com.example.mediasessiontest.adapter

import android.app.Application
import android.util.Log
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.example.mediasessiontest.adapter.BaseBindingAdapter.BaseBindingViewHolder
import androidx.databinding.ObservableArrayList
import com.example.mediasessiontest.adapter.BaseBindingAdapter.ListChangedCallback
import com.example.mediasessiontest.adapter.BaseBindingAdapter
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.databinding.ObservableList.OnListChangedCallback
import androidx.databinding.BindingAdapter

/**
 * 作用:
 */
abstract class BaseBindingAdapter<M, B : ViewDataBinding?>(protected var context: Application?) : RecyclerView.Adapter<BaseBindingViewHolder>() {
    private var mItems: ObservableArrayList<M>?
    protected var mListChangedCallback: ListChangedCallback?
    val items: ObservableArrayList<M>
        get() {
            val m = ObservableArrayList<M>()
            m.addAll(mItems!!)
            return m
        }

    fun setItems(newItems: List<M>?): BaseBindingAdapter<M, B> {
        if (newItems == null) return this
        if (mItems != null) {
            if (mItems!!.size > 0) mItems!!.clear()
            mItems!!.addAll(newItems)
        }
        return this
    }

    class BaseBindingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int {
        return if (mItems == null) 0 else mItems!!.size
    }

    /*
     * 释放资源
     * */
    protected open fun release() {
        if (context != null) {
            context = null
        }
        if (mListChangedCallback != null) {
            mListChangedCallback = null
        }
        if (mItems != null) {
            if (mItems!!.size > 0) {
                mItems!!.clear()
            }
            mItems = null
        }
    }

    /*
    * 视图、数据绑定
    * */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder {
        val binding = DataBindingUtil.inflate<B>(LayoutInflater.from(context),
            getLayoutResId(viewType), parent, false)
        return BaseBindingViewHolder(binding!!.root)
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder, position: Int) {
        val binding = DataBindingUtil.getBinding<B>(holder.itemView)
        onBindItem(binding, mItems!![position], position)
    }

    //子类实现
    @LayoutRes
    protected abstract fun getLayoutResId(ViewType: Int): Int
    protected abstract fun onBindItem(binding: B?, item: M, position: Int)

    /*
    * RecyclerView视图分离与固定
    * */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (mItems != null) mItems!!.addOnListChangedCallback(mListChangedCallback)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (mItems != null) mItems!!.removeOnListChangedCallback(mListChangedCallback)
    }

    /*
    * 处理数据集合变化
    * */
    protected fun onChange(newItems: ObservableArrayList<M>?) {
        resetItems(newItems)
        notifyDataSetChanged()
    }

    protected fun onItemRangeChanged(newItems: ObservableArrayList<M>?, positionStart: Int, itemCount: Int) {
        resetItems(newItems)
        notifyItemRangeChanged(positionStart, itemCount)
    }

    protected fun onItemRangeInserted(newItems: ObservableArrayList<M>?, positionStart: Int, itemCount: Int) {
        resetItems(newItems)
        notifyItemRangeInserted(positionStart, itemCount)
    }

    protected fun onItemRangeMoved(newItems: ObservableArrayList<M>?) {
        resetItems(newItems)
        notifyDataSetChanged()
    }

    protected fun onItemRangeRemoved(newItems: ObservableArrayList<M>?, positionStart: Int, itemCount: Int) {
        resetItems(newItems)
        notifyItemRangeRemoved(positionStart, itemCount)
    }

    protected fun resetItems(newItems: ObservableArrayList<M>?) {
        mItems = newItems
    }

    inner class ListChangedCallback : OnListChangedCallback<ObservableArrayList<M>?>() {
        override fun onChanged(newItems: ObservableArrayList<M>?) {
            Log.d(TAG, "onChanged: ")
            onChange(newItems)
        }

        override fun onItemRangeChanged(newItems: ObservableArrayList<M>?, positionStart: Int, itemCount: Int) {
            this@BaseBindingAdapter.onItemRangeChanged(newItems, positionStart, itemCount)
            Log.d(TAG, "onItemRangeChanged: ")
        }

        override fun onItemRangeInserted(newItems: ObservableArrayList<M>?, positionStart: Int, itemCount: Int) {
            this@BaseBindingAdapter.onItemRangeInserted(newItems, positionStart, itemCount)
            Log.d(TAG, "onItemRangeInserted: ")
        }

        override fun onItemRangeMoved(newItems: ObservableArrayList<M>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            this@BaseBindingAdapter.onItemRangeMoved(newItems)
            Log.d(TAG, "onItemRangeMoved: ")
        }

        override fun onItemRangeRemoved(newItems: ObservableArrayList<M>?, positionStart: Int, itemCount: Int) {
            this@BaseBindingAdapter.onItemRangeRemoved(newItems, positionStart, itemCount)
            Log.d(TAG, "onItemRangeRemoved: ")
        }
    }

    companion object {
        private const val TAG = "BaseBindingAdapter"

        /**
         * 作用: 解决在使用dataBinding 在布局文件给ImageView src属性绑定DrawableResId，不显示相应图片或显示颜色块的问题
         * 参考：https://blog.csdn.net/Ryfall/article/details/
         * 90750270?spm=1001.2101.3001.6650.3&utm_medium=distribute.pc_relevant.none
         * -task-blog-2%7Edefault%7ECTRLIST%7Edefault-3.no_search_link&depth_1-utm_source
         * =distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-3.no_search_link
         * 过程分析：https://blog.csdn.net/zhuhai__yizhi/article/details/52181697 */
        @JvmStatic
        @BindingAdapter("android:src")
        fun setSrc(view: ImageView, resId: Int) {
            //Log.d(TAG, "setSrc: ");
            view.setImageResource(resId)
        }
    }

    init {
        mItems = ObservableArrayList()
        mListChangedCallback = ListChangedCallback()
    }
}