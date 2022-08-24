package com.example.mediasessiontest.bean

/**
 * 作用: 抽象一首歌曲，用于展示或者设置其信息
 */
class MusicBean(
    var id: String,
    var title: String,
    var artist: String,
    var album: String,
    var albumPath: String,
    var path: String,
    var duration: Long
    )