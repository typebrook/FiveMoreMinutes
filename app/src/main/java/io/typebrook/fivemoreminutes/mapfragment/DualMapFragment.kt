package io.typebrook.fivemoreminutes.mapfragment

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.UI
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.verticalLayout

/**
 * Created by pham on 2017/10/9.
 */
class DualMapFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return UI {
            verticalLayout {
                frameLayout { id = ID_MAP1 }
                frameLayout { id = ID_MAP2 }
            }
        }.view
    }

    companion object {
        val ID_MAP1 = 1001
        val ID_MAP2 = 1002
    }
}