package io.typebrook.fivemoreminutes.dialog

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.widget.EditText
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.realm
import io.typebrook.fmmcore.redux.Mode
import io.typebrook.fmmcore.redux.SetMode
import org.jetbrains.anko.*
import java.util.*

class SaveMarkerDialog : DialogFragment() {

    private val currentTime = Date()
    lateinit var markerName: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return alert {
            title = "請為航點命名"
            customView = UI {
                frameLayout {
                    padding = dip(20)
                    markerName = editText {
                        hint = currentTime.toString().substringBefore(" GMT")
                    }
                }
            }.view
            yesButton {
                val focus = mainStore.state.currentControl.focus ?: return@yesButton
                val name = markerName.run {
                    if (text.isNotBlank()) text.toString() else null
                }
                // need to move to redux action
                realm.executeTransaction {
                    it.copyToRealm(focus.apply { this.name = name })
                }
                mainStore dispatch SetMode(Mode.Default)
            }
            cancelButton {}
        }.build() as Dialog
    }
}