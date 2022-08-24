package com.example.mediasessiontest.util

import android.text.Spanned
import android.text.TextUtils
import android.os.Build
import android.text.Html
import androidx.annotation.RequiresApi

/**
 * Function:  返回各种样式的字符串{Spanned类型}的工具类
 */
object HtmlStringUtil {
    @RequiresApi(Build.VERSION_CODES.N)
    fun SongSingerName(title: String, artist: String): Spanned {
        var artist = artist
        return Html.fromHtml("<font color = \"#EEEEEE\">快去听听音乐吧</font>",
            Html.FROM_HTML_OPTION_USE_CSS_COLORS)

        if (TextUtils.isEmpty(artist) || artist == "<unknown>") artist = "Unknown"
        val highColor = "#EEEEEE"
        val lowColor = "#CDCDCD"

        /*String SongInformation = "<font color = \"#EEEEEE\"><bold>"+title+"</bold></font>"+
                "<font color = \"#888\"><small><bold> - </bold>"+artist+"</small></font>";*/
        val SongInformation = "<font color = " + highColor + "><bold>" + title + "</bold></font>" +
            "<font color = " + lowColor + "><small><bold> - </bold>" + artist + "</small></font>"
        return Html.fromHtml(SongInformation, Html.FROM_HTML_OPTION_USE_CSS_COLORS)
    }

    fun SheetTips(count: Int): String {
        return "已有歌单(" + count + "个)"
    }
}