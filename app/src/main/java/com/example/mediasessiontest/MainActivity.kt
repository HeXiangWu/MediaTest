package com.example.mediasessiontest

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
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
        get() = myMediaControllerCallback
    override val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback?
        get() = myMediaBrowserSubscriptionCallback

    private val myMediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            super.onMetadataChanged(metadata)
        }

        override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
            super.onPlaybackStateChanged(playbackState)
            //Log.w(TAG, "onPlaybackStateChanged: "+state);
        }

        override fun onSessionEvent(event: String, extras: Bundle) {
            super.onSessionEvent(event, extras)
        }
    }

    private var myMediaBrowserSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String,
                                      children: List<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
        }

        override fun onError(parentId: String) {
            super.onError(parentId)
        }
    }
}