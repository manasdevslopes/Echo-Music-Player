package com.universe.echo.fragments
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.universe.echo.R

class AboutUsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_about_us, container, false)

        activity.title = "About Us"
//        val img = BitmapFactory.decodeResource(getResources(), R.drawable.manas)
//        val round = RoundedBitmapDrawableFactory.create(getResources(), img)
//        round.isCircular=true
//        imgDev.setImageDrawable(round)

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)

        val item = menu?.findItem(R.id.action_sort)
        item?.isVisible = false
    }
}