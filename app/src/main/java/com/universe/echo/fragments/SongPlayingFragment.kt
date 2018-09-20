package com.universe.echo.fragments


import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.cleveroad.audiovisualization.AudioVisualization
import com.cleveroad.audiovisualization.DbmHandler
import com.cleveroad.audiovisualization.GLAudioVisualizationView
import com.universe.echo.CurrentSongHelper
import com.universe.echo.R
import com.universe.echo.Songs
import com.universe.echo.databases.EchoDatabase
import com.universe.echo.fragments.SongPlayingFragment.Staticated.onSongCompletion
import com.universe.echo.fragments.SongPlayingFragment.Staticated.playNext
import com.universe.echo.fragments.SongPlayingFragment.Staticated.processInformation
import com.universe.echo.fragments.SongPlayingFragment.Staticated.updateTextViews
import com.universe.echo.fragments.SongPlayingFragment.Statified.audioVisualization
import com.universe.echo.fragments.SongPlayingFragment.Statified.currentPosition
import com.universe.echo.fragments.SongPlayingFragment.Statified.currentSongHelper
import com.universe.echo.fragments.SongPlayingFragment.Statified.endTimeText
import com.universe.echo.fragments.SongPlayingFragment.Statified.fab
import com.universe.echo.fragments.SongPlayingFragment.Statified.favouriteContent
import com.universe.echo.fragments.SongPlayingFragment.Statified.fetchSongs
import com.universe.echo.fragments.SongPlayingFragment.Statified.glView
import com.universe.echo.fragments.SongPlayingFragment.Statified.loopImageButton
import com.universe.echo.fragments.SongPlayingFragment.Statified.mediaplayer
import com.universe.echo.fragments.SongPlayingFragment.Statified.myActivity
import com.universe.echo.fragments.SongPlayingFragment.Statified.nextImageButton
import com.universe.echo.fragments.SongPlayingFragment.Statified.playPauseImageButton
import com.universe.echo.fragments.SongPlayingFragment.Statified.previosImageButton
import com.universe.echo.fragments.SongPlayingFragment.Statified.shuffleImageButton
import com.universe.echo.fragments.SongPlayingFragment.Statified.songArtistView
import com.universe.echo.fragments.SongPlayingFragment.Statified.songTitleView
import com.universe.echo.fragments.SongPlayingFragment.Statified.startTimeText
import com.universe.echo.fragments.SongPlayingFragment.Statified.updateSongTime
import kotlinx.android.synthetic.main.fragment_song_playing.view.*
import java.util.*
import java.util.concurrent.TimeUnit

class SongPlayingFragment : Fragment() {

    /*Here you may wonder that why did we create two objects namely Statified and Staticated respectively
    * These objects are created as the variables and functions will be used from another class
    * Now, the question is why did we make two different objects and not one single object
    * This is because we created the Statified object which contains all the variables and
    * the Staticated object which contain all the functions*/

    object Statified {
        var myActivity: Activity? = null
        /*This is the media player variable. We would be using this to play/pause the music*/
        var mediaplayer: MediaPlayer? = null
        /*The different variables defined will be used for their respective purposes*/
        /*Depending on the task they do we name the variables as such so that it gets easier to identify the task they perform*/
        var startTimeText: TextView? = null
        var endTimeText: TextView? = null
        var playPauseImageButton: ImageButton? = null
        var previosImageButton: ImageButton? = null
        var nextImageButton: ImageButton? = null
        var loopImageButton: ImageButton? = null
        var shuffleImageButton: ImageButton? = null
        var seekBar: SeekBar? = null
        var songArtistView: TextView? = null
        var songTitleView: TextView? = null
        var currentPosition: Int = 0
        var fetchSongs: ArrayList<Songs>? = null
        /*The current song helper is used to store the details of the current song being played*/
        var currentSongHelper: CurrentSongHelper? = null
        /*Declaring the variables for using the visualizer*/
        /*Audio Visualization used for the visual aspects of sound*/
        var audioVisualization: AudioVisualization? = null
        /*The visualization view*/
        var glView: GLAudioVisualizationView? = null

        /*Declaring variable for handling the favorite button*/
        var fab: ImageButton? = null

        /*Variable for using DB functions*/
        var favouriteContent: EchoDatabase? = null

        /*Sensor Variables*/
        var mSensorManager: SensorManager? = null
        var mSensorListener: SensorEventListener? = null
        var MY_PREFS_NAME = "ShakeFeature"

        /*Variable used to update the song time*/
        var updateSongTime = object : Runnable {
            override fun run() {
                /*Retrieving the current time position of the media player*/
                val getCurrent = Statified.mediaplayer!!.getCurrentPosition()
                /*The start time is set to the current position of the song
                * The TimeUnit class changes the units to minutes and milliseconds and applied to the string
                * The %d:%d is used for formatting the time strings as 03:45 so that it appears like time*/

//                startTimeText?.setText(String.format("%d:%d",
//                        TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long),
//                        TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long))))
                Statified.startTimeText!!.setText(String.format("%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(getCurrent.toLong() as Long),
                        TimeUnit.MILLISECONDS.toSeconds(getCurrent.toLong() as Long) -
                                TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long))))
                Statified.seekBar?.setProgress(getCurrent.toInt() as Int)
                /*Since updating the time at each second will take a lot of processing, so we perform this task on the different thread using Handler*/
                Handler().postDelayed(this, 1000)
            }
        }
    }


    object Staticated {
        var MY_PREFS_SHUFFLE = "Shuffle feature"
        var MY_PREFS_LOOP = "Loop feature"

        /*Function to handle the event where the song completes playing*/
        fun onSongCompletion() {
            /*If shuffle was on then play a random next song*/
            if (currentSongHelper!!.isShuffle as Boolean) {
                playNext("PlayNextLikeNormalShuffle")
                currentSongHelper!!.isPlaying = true
            } else {
                /*If loop was ON, then play the same song again*/
                if (currentSongHelper!!.isLoop as Boolean) {
                    currentSongHelper!!.isPlaying = true
                    var nextSong = fetchSongs!!.get(currentPosition)

                    currentSongHelper!!.songTitle = nextSong.songTitle
                    currentSongHelper!!.songPath = nextSong.songData
                    currentSongHelper!!.currentPosition = currentPosition
                    currentSongHelper!!.songId = nextSong.songID as Long

                    /*updating the text views for title and artist name*/
                    updateTextViews(currentSongHelper!!.songTitle as String, currentSongHelper!!.songArtist as String)
                    mediaplayer!!.reset()
                    try {
                        mediaplayer!!.setDataSource(myActivity, Uri.parse(currentSongHelper!!.songPath))
                        mediaplayer!!.prepare()
                        mediaplayer!!.start()
                        processInformation(mediaplayer as MediaPlayer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    /*If loop was OFF then normally play the next song*/
                    playNext("PlayNextNormal")
                    currentSongHelper!!.isPlaying = true
                }
            }
            if (favouriteContent!!.checkifIdExists(currentSongHelper!!.songId!!.toInt() as Int) as Boolean) {
                fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_on))
            } else {
                fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_off))
            }
        }

        /*Function to update the views of songs and their artist names*/
        fun updateTextViews(songTitle: String, songArtist: String) {
            var songTitleUpdated = songTitle
            var songArtistUpdated = songArtist
            if (songTitle.equals("<unknown>", true)) {
                songTitleUpdated = "unknown"
            }
            Statified.songTitleView!!.setText(songTitleUpdated)

            if (songArtist.equals("<unknown>", true)) {
                songArtistUpdated = "unknown"
            }
            Statified.songArtistView!!.text = songArtistUpdated
        }

        /*function used to update the time*/
        fun processInformation(mediaPlayer: MediaPlayer) {
            /*Obtaining the final time*/
            val finalTime = mediaPlayer.duration
            /*Obtaining the current position*/
            val startTime = mediaPlayer.currentPosition
            Statified.seekBar!!.max = finalTime
            /*Here we format the time and set it to the start time text*/
            startTimeText?.setText(String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
            )
            /*Similar to above is done for the end time text*/
            endTimeText?.setText(String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong())))
            )
            /*Seekbar has been assigned this time so that it moves according to the time of song*/
            Statified.seekBar!!.setProgress(startTime)
            /*Now this task is synced with the update song time object*/
            Handler().postDelayed(updateSongTime, 1000)
        }

        /*The playNext() function is used to play the next song
   * The playNext() function is called when we tap on the next button*/
        fun playNext(check: String) {
            /*Here we check the value of the string parameter passed*/
            if (check.equals("PlayNextNormal", true)) {
                /*If the check string was PlayNextNormal, we normally move on to the next song*/
                currentPosition = currentPosition + 1
            } else if (check.equals("PlayNextLikeNormalShuffle", true)) {
                /*If the check string was PlayNextLikeNormalShuffle, we then randomly select a song and play it*/
                /*The next steps are used to choose the select a random number
                * First we declare a variable and then initialize it to a random object*/
                var randomObject = Random()
                /*Now here we calculate the random number
                * The nextInt(val n: Int) is used to get a random number between 0(inclusive) and the number passed in this argument(n), exclusive.
                * Here we pass the paramter as the length of the list of the songs fetched
                * We add 1 to the size as the length will be one more than the size. For example if the size of arraylist is 10, then it has items from 0 to 10, which gives the length as 11*/
                var randomPosition = randomObject.nextInt(fetchSongs!!.size.plus(1) as Int)
                /*Now put the current position i.e the position of the song to be played next equal to the random position*/
                currentPosition = randomPosition
            }
            /*Now if the current position equals the length of the i.e the current position points to the end of the list
            * we then make the current position to 0 as no song will be there*/
            if (currentPosition == fetchSongs!!.size) {
                currentPosition = 0
            }
            /*Here we get the details of the song which is played as the next song
           * and update the contents of the current song helper*/
            currentSongHelper!!.isLoop = false
            var nextSong = fetchSongs!!.get(currentPosition)
            currentSongHelper!!.songTitle = nextSong.songTitle
            currentSongHelper!!.songPath = nextSong.songData
            currentSongHelper!!.currentPosition = currentPosition
            currentSongHelper!!.songId = nextSong.songID
            currentSongHelper!!.songArtist = nextSong.artist

            updateTextViews(currentSongHelper!!.songTitle as String, currentSongHelper!!.songArtist as String)

            /*Before playing the song we reset the media player*/
            mediaplayer!!.reset()
            try {
                /*Similar steps which were done when we started the music*/
                mediaplayer!!.setDataSource(myActivity, Uri.parse(currentSongHelper!!.songPath))
                mediaplayer!!.prepare()
                mediaplayer!!.start()
                processInformation(mediaplayer as MediaPlayer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (favouriteContent!!.checkifIdExists(currentSongHelper!!.songId!!.toInt() as Int) as Boolean) {
                fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_on))
            } else {
                fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_off))
            }
        }
    }

    var mAcceleration: Float = 0f
    var mAccelerationCurrent: Float = 0f
    var mAccelerationLast: Float = 0f


    /*Similar onCreateView() method of the fragment, which we used for the MainScreenFragment*/
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view = inflater!!.inflate(R.layout.fragment_song_playing, container, false)

        setHasOptionsMenu(true)

        activity.title = "Now Playing"

        /*Linking views with their ids*/
        Statified.seekBar = view.seekBar
        startTimeText = view.startTime
        endTimeText = view.endTime
        playPauseImageButton = view.playPauseButton
        nextImageButton = view.nextButton
        previosImageButton = view.previosButton
        loopImageButton = view.loopButton
        shuffleImageButton = view.shuffleButton
        songArtistView = view.songArtist
        songTitleView = view.songTitleFavScreen
        glView = view!!.visualizer_view
        fab = view!!.favouriteIcon
        /*Fading the favorite icon*/
        fab!!.alpha = 0.8f
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*Connecting the audio visualization with the view*/
        audioVisualization = glView as AudioVisualization
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as Activity
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        myActivity = activity
    }

    override fun onResume() {
        super.onResume()
        /*When the fragment resumes, it resumes the visualization process*/
        audioVisualization!!.onResume()

        /*Here we register the sensor*/
        Statified.mSensorManager!!.registerListener(Statified.mSensorListener,
                Statified.mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)

    }

    override fun onPause() {
        super.onPause()
        /*Pausing the visualization when fragment pauses to prevent battery drain*/
        audioVisualization!!.onPause()

        /*When fragment is paused, we remove the sensor to prevent the battery drain*/
        Statified.mSensorManager!!.unregisterListener(Statified.mSensorListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        /*Releasing all the resources held by the visualizer when fragment is removed*/
        audioVisualization!!.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*Sensor service is activated when the fragment is created*/
        Statified.mSensorManager = Statified.myActivity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        /*Default values*/
        mAcceleration = 0.0f
        /*We take earth's gravitational value to be default, this will give us good results*/
        mAccelerationCurrent = SensorManager.GRAVITY_EARTH
        mAccelerationLast = SensorManager.GRAVITY_EARTH
        /*Here we call the function*/
        bindShakeListener()
    }

    /*This function is used to create the menu
    * Mainly we create the menu file n XML and inflate it here in this function*/
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu!!.clear()
        inflater!!.inflate(R.menu.song_playing_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /*If any other item present, but action_redirect item have to be shown always than any other other item.**/
    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        val item: MenuItem = menu!!.findItem(R.id.action_redirect)
        item.isVisible = true
//        val item2: MenuItem = menu!!.findItem(R.id.action_sort)
//        item2.isVisible = false
    }

    /*Here we handle the click event of the menu item*/
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
        /*If the id of the item click is action_redirect
         * we navigate back to the list*/
            R.id.action_redirect -> {
                Statified.myActivity!!.onBackPressed()
                return false
            }
        }
        return false
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        /*Initialising the database*/
        favouriteContent = EchoDatabase(myActivity)

        /*Initialising the params of the current song helper object*/
        currentSongHelper = CurrentSongHelper()
        currentSongHelper!!.isPlaying = true
        currentSongHelper!!.isLoop = false
        currentSongHelper!!.isShuffle = false
        var path: String? = null
        var _songTitle: String? = null
        var _songArtist: String? = null
        var songId: Long = 0
        /*See that we have used a try catch block here
        * The reason for doing so is that, it may happen that the bundle object does not have these in it and the app may crash
        * So in order to prevent the crash we use try-catch block. This block is known as the error-handling block*/
        try {
            /*path is retrieved using the same key (path) which was used to send it*/
            path = arguments.getString("path")

            /*song title retrieved with its key songTitle*/
            _songTitle = arguments.getString("songTitle")

            /*song artist with the key songArtist*/
            _songArtist = arguments.getString("songArtist")

            /*song id with the key SongId*/
            songId = arguments.getInt("songId").toLong()

            /*Here we fetch the received bundle data for current position and the list of all songs*/
            currentPosition = arguments.getInt("songPosition")
            fetchSongs = arguments.getParcelableArrayList("songData")

            /*Now store the song details to the current song helper object so that they can be used later*/
            currentSongHelper!!.songPath = path
            currentSongHelper!!.songTitle = _songTitle
            currentSongHelper!!.songArtist = _songArtist
            currentSongHelper!!.songId = songId
            currentSongHelper!!.currentPosition = currentPosition

            updateTextViews(currentSongHelper!!.songTitle as String, currentSongHelper!!.songArtist as String)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        /*Here we check whether we came to the song playing fragment via tapping on a song or by bottom bar*/
        var fromFavBottomBar = arguments.get("FabBottomBar") as? String
        if (fromFavBottomBar != null) {
            /*If we came via bottom bar then the already playing media player object is used*/
            Statified.mediaplayer = FavouriteFragment.Statified.mediaPlayer
        } else {
            /*Else we use the default way*/
            /*here we initialise the media player object*/
            mediaplayer = MediaPlayer()
            /*here we tell the media player object that we would be streaming the music*/
            mediaplayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            /*Here also we use the error-handling as the path we sent may return a null object*/
            try {
                /*The data source set the song to the media player object*/
                mediaplayer!!.setDataSource(myActivity, Uri.parse(path))
                /*Before playing the music we prepare the media player for playback*/
                mediaplayer!!.prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            /*If all of the above goes well we start the music using the start() method*/
            mediaplayer!!.start()
        }

        processInformation(mediaplayer as MediaPlayer)

        if (currentSongHelper!!.isPlaying as Boolean) {
            playPauseImageButton!!.setBackgroundResource(R.drawable.pause_icon)
        } else {
            playPauseImageButton!!.setBackgroundResource(R.drawable.play_icon)
        }

        mediaplayer!!.setOnCompletionListener {
            onSongCompletion()
        }
        clickHandler()

        /*Initializing the handler to handle the visual effects*/
        val visualizationHandler = DbmHandler.Factory.newVisualizerHandler(myActivity as Context, 0)
        /*Linking the audio visualization with the handler*/
        audioVisualization!!.linkTo(visualizationHandler)

        /*Now we want that when if user has turned shuffle or loop ON, then these settings should persist even if the app is restarted after closing
        * This is done with the help of Shared Preferences
        * Shared preferences are capable of storing small amount of data in the form of key-value pair*/

        /*Here we initialize the preferences for shuffle in a private mode
        * Private mode is chosen so that so other app us able to read the preferences apart from our app*/
        var prefsForShuffle = myActivity!!.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)

        /*Here we extract the value of preferences and check if shuffle was ON or not*/
        var isShuffleAllowed = prefsForShuffle.getBoolean("feature", false)
        if (isShuffleAllowed as Boolean) {
            /*if shuffle was found activated, then we change the icon color and tun loop OFF*/
            currentSongHelper!!.isShuffle = true
            currentSongHelper!!.isLoop = false
            shuffleImageButton!!.setBackgroundResource(R.drawable.shuffle_icon)
            loopImageButton!!.setBackgroundResource(R.drawable.loop_white_icon)
        } else {
            /*Else default is set*/
            currentSongHelper!!.isShuffle = false
            shuffleImageButton!!.setBackgroundResource(R.drawable.shuffle_white_icon)
        }
        /*Similar to the shuffle we check the value for loop activation*/
        var prefsForLoop = myActivity!!.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)
        /*Here we extract the value of preferences and check if loop was ON or not*/
        var isLoopAllowed = prefsForLoop.getBoolean("feature", false)
        if (isLoopAllowed as Boolean) {
            /*If loop was activated we change the icon color and shuffle is turned OFF */
            currentSongHelper!!.isShuffle = false
            currentSongHelper!!.isLoop = true
            shuffleImageButton!!.setBackgroundResource(R.drawable.shuffle_white_icon)
            loopImageButton!!.setBackgroundResource(R.drawable.loop_icon)
        } else {
            /*Else defaults are used*/
            loopImageButton!!.setBackgroundResource(R.drawable.loop_white_icon)
            currentSongHelper!!.isLoop = false
        }

        /*Here we check that if the song playing is a favorite, then we show a red colored heart indicating favorite else only the heart boundary
        * This action is performed whenever a new song is played, hence this will done in the playNext(), playPrevious() and onSongComplete() methods*/
        if (favouriteContent!!.checkifIdExists(currentSongHelper!!.songId!!.toInt() as Int) as Boolean) {
            fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_on))
        } else {
            fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_off))
        }
    }

    /*A new click handler function is created to handle all the click functions in the song playing fragment*/
    fun clickHandler() {
        /*Here we handle the click of the favorite icon
        * When the icon was clicked, if it was red in color i.e. a favorite song then we remove the song from favorites*/
        fab!!.setOnClickListener({
            if (favouriteContent!!.checkifIdExists(currentSongHelper!!.songId!!.toInt() as Int) as Boolean) {
                fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_off))
                favouriteContent!!.deleteFavourites(currentSongHelper!!.songId!!.toInt() as Int)
                /*Toast is prompt message at the bottom of screen indicating that an action has been performed*/
                Toast.makeText(myActivity, "Remove from Favourites", Toast.LENGTH_SHORT).show()
            } else {
                /*If the song was not a favorite, we then add it to the favorites using the method we made in our database*/
                fab!!.setImageDrawable(ContextCompat.getDrawable(myActivity, R.drawable.favorite_on))
                favouriteContent!!.storeAsFavourite(currentSongHelper!!.songId!!.toInt() as Int, currentSongHelper!!.songTitle!!, currentSongHelper!!.songArtist!!, currentSongHelper!!.songPath!!)
                Toast.makeText(myActivity, "Added to Favourites", Toast.LENGTH_SHORT).show()

            }
        })
        shuffleImageButton!!.setOnClickListener({
            /*Initializing the shared preferences in private mode
            * edit() used so that we can overwrite the preferences*/
            var editorShuffle = myActivity!!.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE).edit()
            var editorLoop = myActivity!!.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE).edit()

            if (currentSongHelper!!.isShuffle as Boolean) {
                shuffleImageButton!!.setBackgroundResource(R.drawable.shuffle_white_icon)
                currentSongHelper!!.isShuffle = false
                /*If shuffle was activated previously, then we deactivate it*/
                /*The putBoolean() method is used for saving the boolean value against the key which is feature here*/

                /*Now the preferences against the block Shuffle feature will have a key: feature and its value: false*/
                editorShuffle!!.putBoolean("feature", false)
                editorShuffle.apply()
            } else {
                currentSongHelper!!.isShuffle = true
                currentSongHelper!!.isLoop = false
                shuffleImageButton!!.setBackgroundResource(R.drawable.shuffle_icon)
                loopImageButton!!.setBackgroundResource(R.drawable.loop_white_icon)

                /*Else shuffle is activated and if loop was activated then loop is deactivated*/
                editorShuffle!!.putBoolean("feature", true)
                editorShuffle.apply()
                /*Similar to shuffle, the loop feature has a key:feature and its value:false*/
                editorLoop!!.putBoolean("feature", false)
                editorLoop.apply()
            }
        })
        /*Here we set the click listener to the next button*/
        Statified.nextImageButton!!.setOnClickListener({
            /*We set the player to be playing by setting isPlaying to be true*/
            currentSongHelper!!.isPlaying = true
            playPauseImageButton?.setImageResource(R.drawable.pause_icon)
            /*First we check if the shuffle button was enabled or not*/
            if (currentSongHelper!!.isShuffle as Boolean) {
                /*If yes, then  we play next song randomly
                * The check string is passed as the PlayNextLikeNormalShuffle which plays the random next song*/
                playNext("PlayNextLikeNormalShuffle")
            } else {
                /*If shuffle was not enabled then we normally play the next song
                * The check string passed is the PlayNextNormal which serves the purpose*/
                playNext("PlayNextNormal")
            }

        })
        /*Here we set the click listener to the next button*/
        previosImageButton!!.setOnClickListener({
            /*We set the player to be playing by setting isPlaying to be true*/
            currentSongHelper!!.isPlaying = true
            /*First we check if the loop is on or not*/
            if (currentSongHelper!!.isLoop as Boolean) {
                /*If the loop was on we turn it off*/
                loopImageButton!!.setBackgroundResource(R.drawable.loop_white_icon)
            }
            /*After all of the above is done we then play the previous song using the playPrevious() function*/
            playPrevios()
        })
        /*Here we handle the click on the loop button*/
        loopImageButton!!.setOnClickListener({
            var editorShuffle = myActivity!!.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE).edit()
            var editorLoop = myActivity!!.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE).edit()
            /*if loop was enabled, we turn it off and vice versa*/
            if (currentSongHelper!!.isLoop as Boolean) {
                /*Making the isLoop false*/
                currentSongHelper!!.isLoop = false
                /*We change the color of the icon*/
                loopImageButton!!.setBackgroundResource(R.drawable.loop_white_icon)

                editorLoop!!.putBoolean("feature", false)
                editorLoop.apply()
            } else {
                /*If loop was not enabled when tapped, we enable it and make the isLoop to true*/
                currentSongHelper!!.isLoop = true
                /*Loop and shuffle won't work together so we put shuffle false irrespective of the whether it was on or not*/
                currentSongHelper!!.isShuffle = false
                /*Loop button color changed to mark it ON*/
                loopImageButton!!.setBackgroundResource(R.drawable.loop_icon)
                /*Changing the shuffle button to white, no matter which color it was earlier*/
                shuffleImageButton!!.setBackgroundResource(R.drawable.shuffle_white_icon)

                editorShuffle!!.putBoolean("feature", false)
                editorShuffle.apply()
                editorLoop!!.putBoolean("feature", true)
                editorLoop.apply()
            }
        })
        /*Here we handle the click event on the play/pause button*/
        playPauseImageButton!!.setOnClickListener({
            /*if the song is already playing and then play/pause button is tapped
            * then we pause the media player and also change the button to play button*/
            if (mediaplayer!!.isPlaying as Boolean) {
                mediaplayer!!.pause()
                currentSongHelper!!.isPlaying = false
                playPauseImageButton!!.setBackgroundResource(R.drawable.play_icon)
                /*If the song was not playing the, we start the music player and
                * change the image to pause icon*/
            } else {
                mediaplayer!!.start()
                currentSongHelper!!.isPlaying = true
                playPauseImageButton!!.setBackgroundResource(R.drawable.pause_icon)
            }
        })
    }


    /*The function playPrevious() is used to play the previous song again*/
    fun playPrevios() {
        /*Decreasing the current position by 1 to get the position of the previous song*/
        currentPosition = currentPosition - 1
        /*If the current position becomes less than 1, we make it 0 as there is no index as -1*/
        if (currentPosition == -1) {
            currentPosition = 0
        }
        if (currentSongHelper!!.isPlaying as Boolean) {
            playPauseImageButton!!.setBackgroundResource(R.drawable.pause_icon)
        } else {
            playPauseImageButton!!.setBackgroundResource(R.drawable.play_icon)
        }
        currentSongHelper!!.isLoop = false
        /*Similar to the playNext() function defined above*/
        var nextSong = fetchSongs!!.get(currentPosition)
        currentSongHelper!!.songTitle = nextSong.songTitle
        currentSongHelper!!.songPath = nextSong.songData
        currentSongHelper!!.currentPosition = currentPosition
        currentSongHelper!!.songId = nextSong.songID

        updateTextViews(currentSongHelper!!.songTitle as String, currentSongHelper!!.songArtist as String)

        mediaplayer!!.reset()
        try {
            mediaplayer!!.setDataSource(myActivity, Uri.parse(currentSongHelper!!.songPath))
            mediaplayer!!.prepare()
            mediaplayer!!.start()
            processInformation(mediaplayer as MediaPlayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (favouriteContent!!.checkifIdExists(currentSongHelper!!.songId!!.toInt() as Int) as Boolean) {
            fab!!.setImageResource(R.drawable.favorite_on)
        } else {
            fab!!.setImageResource(R.drawable.favorite_off)
        }
    }

    /*This function handles the shake events in order to change the songs when we shake the phone*/
    fun bindShakeListener() {
        /*The sensor listener has two methods used for its implementation i.e. OnAccuracyChanged() and onSensorChanged*/
        Statified.mSensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                /*We do not need to check or work with the accuracy changes for the sensor*/
            }

            override fun onSensorChanged(event: SensorEvent?) {
                /*We need this onSensorChanged function
                *This function is called when there is a new sensor event*/
                /*The sensor event has 3 dimensions i.e. the x, y and z in which the changes can occur*/
                val x = event!!.values[0]
                val y = event.values[1]
                val z = event.values[2]

                /*Now lets see how we calculate the changes in the acceleration*/
                /*Now we shook the phone so the current acceleration will be the first to start with*/
                mAccelerationLast = mAccelerationCurrent
                /*Since we could have moved the phone in any direction, we calculate the Euclidean distance to get the normalized distance*/
                mAccelerationCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                /*Delta gives the change in acceleration*/
                val delta = mAccelerationCurrent - mAccelerationLast
                /*Here we calculate thelower filter
               * The written below is a formula to get it*/
                mAcceleration = mAcceleration * 0.9f + delta
                /*We obtain a real number for acceleration
                * and we check if the acceleration was noticeable, considering 12 here*/
                if (mAcceleration > 12) {
                    /*If the accel was greater than 12 we change the song, given the fact our shake to change was active*/
                    val prefs = Statified.myActivity!!.getSharedPreferences(Statified.MY_PREFS_NAME, Context.MODE_PRIVATE)
                    val isAllowed = prefs!!.getBoolean("feature", false)
                    if (isAllowed as Boolean) {
                        Staticated.playNext("PlayNextNormal")
                    }
                }
            }
        }
    }
}