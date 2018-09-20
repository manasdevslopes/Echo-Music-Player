package com.universe.echo.adapters

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.universe.echo.R
import com.universe.echo.Songs
import com.universe.echo.fragments.SongPlayingFragment
import kotlinx.android.synthetic.main.row_custom_mainscreen_adapter.view.*


class FavoriteAdapter (_songDetails: ArrayList<Songs>, _context: Context) : RecyclerView.Adapter<FavoriteAdapter.myViewHolder>() {


    /*Local variables used for storing the data sent from the fragment to be used in the adapter
    * These variables are initially null*/
    var songDetails: ArrayList<Songs>? = null
    var mContext: Context? = null

    /*In the init block we assign the data received from the params to our local variables*/
    init {
        this.songDetails = _songDetails
        this.mContext = _context
    }

    /*This function is used to create the view for the single row of the recycler view. We inflate the view used for single row inside this method.
    * Let's discuss the params of this method:
    * i) parent: ViewGroup? ->  The view group is the base class for layouts and views containers. Here the parent is the view group into which the new view will be added
    * ii) viewType: Int -> The type of the view to be inflated*/
    /*This has the same implementation which we did for the navigation drawer adapter*/
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): myViewHolder {
        /*Here we inflate our view taking the context from the parent. The inflate() function takes the resource(R.layout.row_custom_navigationdrawer)
        * sets it to the parent and does not attach this to the root. You can skip the details of this as of now*/
        val itemView = LayoutInflater.from(parent!!.context).inflate(R.layout.row_custom_mainscreen_adapter, parent, false)
        /*Here we pass this view into the holder and return that and our view is created. The below tow lines can be reduced as
        * return NavViewHolder(itemView)*/
        val returnThis = myViewHolder(itemView)
        return returnThis
    }

    /*The onBindViewHolder() method is used to display the data at the specified position.
    * The params i.e. holder and position are used to set the data and the position of that data within the recycler view*/
    override fun onBindViewHolder(holder: myViewHolder?, position: Int) {
        val songObject = songDetails!!.get(position)

        /*The holder object of our MyViewHolder class has two properties i.e
       * trackTitle for holding the name of the song and
       * trackArtist for holding the name of the artist*/
        holder!!.trackTitle!!.text = songObject.songTitle
        holder.trackArtist!!.text = songObject.artist

        /*Handling the click event i.e. the action which happens when we click on any song*/
        holder.contentHolder!!.setOnClickListener({

            /*Let's discuss this peice of code*/
            /*Firstly we define an object of the SongPlayingFragment*/
            val songPlayingFragment = SongPlayingFragment()

            /*A bundle is used to transfer data from one point in your activity to another
             * Here we create an object of Bundle to send the song details to the fragment so that we can display the song details there and also play the song*/
            var args = Bundle()
            /*putString() function is used for adding a string to the bundle object
             * the string written in green is the name of the string which is placed in the bundle object with the value of that string written alongside
             * Note: Remember the name of the strings/entities you place inside the bundle object as you will retrieve them later using the same name. And these names are case-sensitive*/
            args.putString("songArtist", songObject.artist)
            args.putString("path", songObject.songData)
            args.putString("songTitle", songObject.songTitle)
            args.putInt("songId", songObject.songID.toInt())
            args.putInt("songPosition", position)
            /*Here the complete array list is sent*/
            args.putParcelableArrayList("songData", songDetails)

            /*Note: We have only added the details to the bundle object but we still have to pass this bundle to the song playing fragment.*/
            songPlayingFragment.arguments = args

            /*Now after placing the song details inside the bundle, we inflate the song playing fragment*/
            (mContext as FragmentActivity).supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.details_fragments, songPlayingFragment)
                    .addToBackStack("SongPlayingFragmentFavorite")
                    .commit()
        })
    }


    /*This function returns the number elements present in our recycler view. The number of these items can be calculated by the number of items in our arraylist(contentList)*/
    override fun getItemCount(): Int {
        /*Here we return the size of the list we used.*/
        /*If the array list for the songs is null i.e. there are no songs in your device
        * then we return 0 and no songs are displayed*/
        if (songDetails == null) {
            return 0
        }
        /*Else we return the total size of the song details which will be the total number of song details*/
        else {
            return (songDetails as ArrayList<Songs>).size
        }
    }

    /*Class for creating a view holder for our recycler view. This class sets up the single object for our recycler view*/
    /*Every view holder class we create will serve the same purpose as it did when we created it for the navigation drawer*/
    class myViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        /*Declaring the widgets and the layout used*/
        var trackTitle: TextView? = null
        var trackArtist: TextView? = null
        var contentHolder: RelativeLayout? = null

        /*Constructor initialisation for the variables*/
        init {
            trackTitle = itemView!!.trackTitle
            trackArtist = itemView.trackArtist
            contentHolder = itemView.contentRow
        }
    }
}
