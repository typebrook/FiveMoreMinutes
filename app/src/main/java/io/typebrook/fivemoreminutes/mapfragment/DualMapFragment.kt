package io.typebrook.fivemoreminutes.mapfragment

import android.app.Fragment
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.typebrook.fivemoreminutes.mainStore
import org.jetbrains.anko.*
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/10/9.
 */
class DualMapFragment : Fragment(), StoreSubscriber<Int> {

    private var firstMap: Fragment? = null
    private var secondMap: Fragment? = null

    private lateinit var currentMap: TextView

    fun insertMap(map1: Fragment, map2: Fragment) {
        this.firstMap = map1
        this.secondMap = map2
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return UI {
            relativeLayout {
                when (configuration.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> verticalLayout {
                        frameLayout { id = ID_FIRST_MAP }.lparams(weight = 1f)
                        frameLayout { id = ID_SECOND_MAP }.lparams(weight = 1f)
                    }
                    else -> linearLayout {
                        frameLayout { id = ID_FIRST_MAP }.lparams(weight = 1f)
                        frameLayout { id = ID_SECOND_MAP }.lparams(weight = 1f)
                    }
                }

                currentMap = textView {
                    text = mainStore.state.mapState.currentMapNum.toString()
                    backgroundColor = Color.parseColor("#80FFFFFF")
                }.lparams { centerInParent() }
            }
        }.view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction()
                .apply { firstMap?.let { replace(ID_FIRST_MAP, it) } }
                .apply { secondMap?.let { replace(ID_SECOND_MAP, it) } }
                .commit()

        mainStore.subscribe(this){subscription ->
            subscription.select { it.mapState.currentMapNum }.skipRepeats()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainStore.unsubscribe(this)
    }

    override fun newState(state: Int) {
        currentMap.text = state.toString()
    }

    companion object {
        private val ID_FIRST_MAP = 3001
        private val ID_SECOND_MAP = 3002
    }
}