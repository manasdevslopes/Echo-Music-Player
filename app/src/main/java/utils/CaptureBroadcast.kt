package com.universe.echo.utils

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.universe.echo.R
import com.universe.echo.activities.MainActivity
import com.universe.echo.fragments.SongPlayingFragment

/*Here we create the Broadcast receiver class
* A broadcast receiver receives the events which happen outside the app
* Here we going to capture the calling event i.e.
* If the user gets a call then music player should pause or
* if the user starts calling someone then also the same should happen*/
class CaptureBroadcast : BroadcastReceiver() {
    /*The broadcast receiver has a mandatory method called the onReceive() method
   * The onReceive() method receives the intent outside the app and performs the functions accordingly
   * Here the intent will be the calling state*/
    override fun onReceive(context: Context?, intent: Intent?) {
        /*Here we check whether the user has an outgoing call or not*/
        if (intent?.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            try {
                MainActivity.Statified.notificationManager?.cancel(1978)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                /*If the media player was playing we pause it and change the play/pause button*/
                if (SongPlayingFragment.Statified.mediaplayer?.isPlaying as Boolean) {
                    SongPlayingFragment.Statified.mediaplayer?.pause()
                    SongPlayingFragment.Statified.playPauseImageButton?.setImageResource(R.drawable.play_icon)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            /*Here we use the telephony manager to get the access for the service*/
            val tm: TelephonyManager = context?.getSystemService(Service.TELEPHONY_SERVICE) as TelephonyManager
            when (tm?.callState) {
            /*We check the call state and if the call is ringing i.e. the user has an incoming call
            * then also we pause the media player*/
                TelephonyManager.CALL_STATE_RINGING -> {
                    try {
                        MainActivity.Statified.notificationManager?.cancel(1978)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        if (SongPlayingFragment.Statified.mediaplayer?.isPlaying as Boolean) {
                            SongPlayingFragment.Statified.mediaplayer?.pause()
                            SongPlayingFragment.Statified.playPauseImageButton?.setImageResource(R.drawable.play_icon)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            /*Else we do nothing*/
                else -> {

                }
            }
        }
    }
}