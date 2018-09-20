package com.universe.echo.fragments


import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.universe.echo.R
import com.universe.echo.Songs
import com.universe.echo.adapters.FavoriteAdapter
import com.universe.echo.databases.EchoDatabase
import kotlinx.android.synthetic.main.fragment_favourite.view.*


/**
 * A simple [Fragment] subclass.
 */
class FavouriteFragment : Fragment() {

    var myActivity: Activity? = null
    var recyclerView: RecyclerView? = null
    var nowPlayingBottomBar: RelativeLayout? = null
    var playPauseButton: ImageButton? = null
    var songTitle: TextView? = null
    var noFavourites: TextView? = null

    var trackPosition: Int = 0

    var favoriteContent: EchoDatabase? = null

    var refreshList: ArrayList<Songs>? = null
    var getListFromDatabse: ArrayList<Songs>? = null

    object Statified {
        var mediaPlayer: MediaPlayer? = null

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_favourite, container, false)

        activity.title = "Favorites"

        recyclerView = view.favouriteRecycler as RecyclerView
        nowPlayingBottomBar = view.hiddenBarFavScreen as RelativeLayout
        playPauseButton = view.playPauseButton as ImageButton
        songTitle = view.songTitleFavScreen as TextView
        noFavourites = view.noFavourite as TextView
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as Activity
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        myActivity = activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        favoriteContent = EchoDatabase(myActivity)
        display_favorites_by_searching()
        bottomBarSetup()

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        val item = menu?.findItem(R.id.action_sort)
        item?.isVisible = false
    }

    /*As the name suggests, this function is used to fetch the songs present in your phones and returns the arraylist of the same*/
    fun getSongsFromPhone(): ArrayList<Songs> {
        val arrayList = ArrayList<Songs>()

        /*A content resolver is used to access the data present in your phone
         * In this case it is used for obtaining the songs present your phone*/
        val contentResolver = myActivity!!.contentResolver

        /*Here we are accessing the Media class of Audio class which in turn a class of Media Store, which contains information about all the media files present
         * on our mobile device*/
        val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        /*Here we make the request of songs to the content resolver to get the music files from our device*/
        val songCursor = contentResolver?.query(songUri, null, null, null, null)

        /*In the if condition we check whether the number of music files are null or not. The moveToFirst() function returns the first row of the results*/
        if (songCursor != null && songCursor.moveToFirst()) {
            val songId = songCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val songData = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val dateIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            /*moveToNext() returns the next row of the results. It returns null if there is no row after the current row*/
            while (songCursor.moveToNext()) {
                val currentId = songCursor.getLong(songId)
                val currentTitle = songCursor.getString(songTitle)
                val currentArtists = songCursor.getString(songArtist)
                val currentData = songCursor.getString(songData)
                val currentDate = songCursor.getLong(dateIndex)

                /*Adding the fetched songs to the arraylist*/
                arrayList.add(Songs(currentId, currentTitle, currentArtists, currentData, currentDate))
            }
        }
        /*Returning the arraylist of songs*/
        return arrayList
    }

    /*The bottom bar setup function is used to place the bottom bar on the favorite screen when we come back from the song playing screen to the favorite screen*/
    fun bottomBarSetup() {
        try {
            /*Calling the click handler function will help us handle the click events of the bottom bar*/
            bottomBarClickHandler()
            /*We fetch the song title with the help of the current song helper and set it to the song title in our bottom bar*/
            songTitle!!.setText(SongPlayingFragment.Statified.currentSongHelper!!.songTitle)

            /*If we are the on the favorite screen and not on the song playing screen when the song finishes
            * we want the changes in the song to reflect on the favorite screen hence we call the onSongComplete() function which help us in maintaining consistency*/
            SongPlayingFragment.Statified.mediaplayer!!.setOnCompletionListener({
                songTitle!!.setText(SongPlayingFragment.Statified.currentSongHelper!!.songTitle)
                SongPlayingFragment.Staticated.onSongCompletion()
            })
            /*While coming back from the song playing screen
            * if the song was playing then only the bottom bar is placed, else not placed*/
            if (SongPlayingFragment.Statified.mediaplayer!!.isPlaying as Boolean) {
                nowPlayingBottomBar!!.visibility = View.VISIBLE
            } else {
                nowPlayingBottomBar!!.visibility = View.INVISIBLE
            }
            /*Since we are dealing with the media player object which can be null, hence we handle all such exceptions using the try-catch block*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*The bottomBarClickHandler() function is used to handle the click events on the bottom bar*/
    fun bottomBarClickHandler() {
        /*We place a click listener on the bottom bar*/
        nowPlayingBottomBar!!.setOnClickListener({
            /*Using the same media player object*/
            Statified.mediaPlayer = SongPlayingFragment.Statified.mediaplayer
            /*Firstly we define an object of the SongPlayingFragment*/
            val songPlayingFragment = SongPlayingFragment()

            /*A bundle is used to transfer data from one point in your activity to another
             * Here we create an object of Bundle to send the song details to the fragment so that we can display the song details there and also play the song*/
            val args = Bundle()
            /*putString() function is used for adding a string to the bundle object
             * the string written in green is the name of the string which is placed in the bundle object with the value of that string written alongside
             * Note: Remember the name of the strings/entities you place inside the bundle object as you will retrieve them later using the same name. And these names are case-sensitive*/

            /*Here when we click on the bottom bar, we navigate to the song playing fragment
           * Since we want the details of the same song which is playing to be displayed in the song playing fragment
           * we pass the details of the current song being played to the song playing fragment using Bundle*/
            args.putString("songArtist", SongPlayingFragment.Statified.currentSongHelper!!.songArtist)
            args.putString("path", SongPlayingFragment.Statified.currentSongHelper!!.songPath)
            args.putString("songTitle", SongPlayingFragment.Statified.currentSongHelper!!.songTitle)
            args.putInt("songId", SongPlayingFragment.Statified.currentSongHelper!!.songId.toInt() as Int)
            args.putInt("songPosition", SongPlayingFragment.Statified.currentSongHelper!!.currentPosition.toInt() as Int)
            /*Here the complete array list is sent*/
            args.putParcelableArrayList("songData", SongPlayingFragment.Statified.fetchSongs)
            /*Here we put the additional string in the bundle
            * this tells us that the bottom bar was successfully setup*/
            args.putString("FabBottomBar", "Success")

            /*Here we pass the bundle object to the song playing fragment*/
            songPlayingFragment.arguments = args

            /*The below lines are now familiar
            * These are used to open a fragment*/
            fragmentManager.beginTransaction()
                    .replace(R.id.details_fragments, songPlayingFragment)
                    /*The below piece of code is used to handle the back navigation
                    * This means that when you click the bottom bar and move on to the next screen
                    * on pressing back button you navigate to the screen you came from*/
                    .addToBackStack("SongPlayingFragment")
                    .commit()
        })
        /*Apart from the click on the bottom bar we have a play/pause button in our bottom bar
        * This button is used to play or pause the media player*/
        playPauseButton!!.setOnClickListener({
            if (SongPlayingFragment.Statified.mediaplayer!!.isPlaying as Boolean) {
                /*If the song was already playing, we then pause it and save the it's position
                * and then change the button to play button*/
                SongPlayingFragment.Statified.mediaplayer!!.pause()
                trackPosition = SongPlayingFragment.Statified.mediaplayer!!.getCurrentPosition() as Int
                playPauseButton!!.setBackgroundResource(R.drawable.play_icon)
            } else {
                /*If the music was already paused and we then click on the button
               * it plays the song from the same position where it was paused
               * and change the button to pause button*/
                SongPlayingFragment.Statified.mediaplayer!!.seekTo(trackPosition)
                SongPlayingFragment.Statified.mediaplayer!!.start()
                playPauseButton!!.setBackgroundResource(R.drawable.pause_icon)
            }
        })
    }

    fun display_favorites_by_searching() {
        if (favoriteContent!!.checkSize() as Int > 0) {
            refreshList = ArrayList<Songs>()
            getListFromDatabse = favoriteContent!!.queryDBList()
            var fetchListFromDevice = getSongsFromPhone()
            if (fetchListFromDevice != null) {
                for (i in 0..fetchListFromDevice!!.size - 1) {
                    for (j in 0..getListFromDatabse!!.size - 1) {
                        if ((getListFromDatabse!!.get(j)!!.songID) == (fetchListFromDevice!!.get(i)!!.songID)) {
                            refreshList!!.add((getListFromDatabse as ArrayList<Songs>)[j])
                        }
                    }
                }
            } else {

            }
            if (refreshList == null) {
                recyclerView!!.visibility = View.INVISIBLE
                noFavourites!!.visibility = View.VISIBLE
            } else {
                var favoriteAdapter = FavoriteAdapter(refreshList as ArrayList<Songs>, myActivity as Context)
                val mLayoutManager = LinearLayoutManager(activity)
                recyclerView!!.layoutManager = mLayoutManager
                recyclerView!!.itemAnimator = DefaultItemAnimator()
                recyclerView!!.adapter = favoriteAdapter
                recyclerView!!.setHasFixedSize(true)
            }
        } else {
            recyclerView!!.visibility = View.INVISIBLE
            noFavourites!!.visibility = View.VISIBLE
        }

    }
}
