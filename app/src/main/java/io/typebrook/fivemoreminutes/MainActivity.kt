package io.typebrook.fivemoreminutes

import android.os.Bundle
import android.support.v4.app.FragmentActivity

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.container, GoogleMapFragment(), "map fragment")
        transaction.commit()
    }
}
