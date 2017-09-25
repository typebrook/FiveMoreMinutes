package io.typebrook.fivemoreminutes

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import io.typebrook.fivemoreminutes.mapfragment.MapBoxMapFragment
import io.typebrook.fivemoreminutes.ui.ActivityUI
import org.jetbrains.anko.find
import org.jetbrains.anko.setContentView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityUI().setContentView(this)

        fragmentManager.beginTransaction()
                .add(ActivityUI.ID_CONTAINER, MapBoxMapFragment(), "map fragment")
                .commit()
    }
}
