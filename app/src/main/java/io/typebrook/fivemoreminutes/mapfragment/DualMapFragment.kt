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
import io.typebrook.fmmcore.redux.SwitchMap
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/10/9.
 */
class DualMapFragment() : Fragment(), StoreSubscriber<Int> {

    private var firstMap: Fragment? = null
    private var secondMap: Fragment? = null

    private lateinit var currentMap: TextView

    constructor(map1: Fragment, map2: Fragment) : this() {
        this.firstMap = map1
        this.secondMap = map2
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return UI {
            relativeLayout {
                when (configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> linearLayout {
                        frameLayout { id = ID_FIRST_MAP }.lparams(weight = 1f)
                        frameLayout { id = ID_SECOND_MAP }.lparams(weight = 1f)
                    }
                    else -> verticalLayout {
                        frameLayout { id = ID_FIRST_MAP }.lparams(weight = 1f)
                        frameLayout { id = ID_SECOND_MAP }.lparams(weight = 1f)
                    }
                }

                currentMap = textView {
                    text = mainStore.state.currentMapNum.toString()
                    backgroundColor = Color.parseColor("#80FFFFFF")
                    onClick { mainStore.dispatch(SwitchMap()) }
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
            subscription.select { it.currentMapNum }.skipRepeats()
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