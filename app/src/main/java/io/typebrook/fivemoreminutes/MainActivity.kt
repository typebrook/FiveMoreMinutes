package io.typebrook.fivemoreminutes

import android.app.Activity
import android.os.Bundle
import org.jetbrains.anko.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ID_CONTAINER = 1
        verticalLayout {
            id = ID_CONTAINER
        }

        fragmentManager.beginTransaction()
                .add(ID_CONTAINER, GoogleMapFragment(), "map fragment")
                .commit()
    }
}
