package io.typebrook.fivemoreminutes

import android.app.Activity
import android.os.Bundle
import io.typebrook.fivemoreminutes.ui.ActivityUI
import org.jetbrains.anko.setContentView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityUI().setContentView(this)

        fragmentManager.beginTransaction()
                .add(ActivityUI.ID_CONTAINER, GoogleMapFragment(), "map fragment")
                .commit()
    }
}
