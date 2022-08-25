package com.example.mediasessiontest

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import com.example.mediasessiontest.viewmodel.MusicViewModel
import java.util.*

class MainActivity : BaseActivity<MusicViewModel>() {

    private val mMusicViewModel: MusicViewModel? = null
    private val mTimer: Timer? = null
    private val mIntentMusic: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override val controllerCallback: MediaControllerCompat.Callback?
        get() = TODO("Not yet implemented")
    override val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback?
        get() = TODO("Not yet implemented")
}