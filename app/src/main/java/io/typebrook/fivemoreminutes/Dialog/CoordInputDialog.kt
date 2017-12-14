package io.typebrook.fivemoreminutes.Dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.widget.EditText
import io.realm.Realm
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.ui.ActivityUI
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.SetProjection
import org.jetbrains.anko.*

/**
 * Created by pham on 2017/11/24.
 */
class CoordInputDialog : DialogFragment() {

    private val crs get() = mainStore.state.datum
    private val converter get() = Datum.generateConverter(crs, WGS84_Degree)
    private lateinit var xValue: EditText
    private lateinit var yValue: EditText

    private val coordList: List<Datum>
        get() {
            val realm = Realm.getDefaultInstance()
            val crsInRealm = realm.where(Datum::class.java).findAll().toList()

            return listOf(WGS84_Degree, WGS84_DMS, TWD97, TWD67) + crsInRealm
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(mainStore.state.datum.displayName)
                .setView(InputBox)
                .setPositiveButton("GOTO") { _, _ ->
                    val xy = try {
                        val rawXY = converter(extractCoor(xValue.text) to extractCoor(yValue.text))
                        if (!isValidInWGS84(rawXY)) throw Error()
                        rawXY
                    } catch (e: Throwable) {
                        toast("Invalid Number")
                        return@setPositiveButton
                    }
                    val target = CameraState(xy.second, xy.first, mainStore.state.currentCamera.zoom)
                    mainStore.state.currentMap.mapControl.animateCamera(target, 600)
                }
                .setNeutralButton("change CRS") { _, _ ->
                    val owner = activity
                    val choices = coordList.map { it.displayName } + "+ Add New"
                    selector("座標系統", choices) { _, index ->
                        if (index > coordList.lastIndex) {
                            CrsCreateDialog().show(owner.fragmentManager, null)
                        } else {
                            val selectedProj = coordList[index]
                            mainStore.dispatch(SetProjection(selectedProj))
                        }
                    }
                }
                .apply {
                    if (!crs.isManaged) return@apply
                    setNegativeButton("刪除") { _, _ ->
                        val realm = Realm.getDefaultInstance()
                        realm.executeTransaction {
                            val abandonCrs = crs
                            mainStore.dispatch(SetProjection(WGS84_Degree))
                            abandonCrs.deleteFromRealm()
                        }
                    }
                }
                .show()
    }

    private val InputBox by lazy {
        UI {
            verticalLayout {
                padding = 25
                xValue = editText {
                    hint = "120.960000"
                    inputType = InputType.TYPE_CLASS_PHONE
                }
                yValue = editText {
                    hint = "23.760000"
                    inputType = InputType.TYPE_CLASS_PHONE
                }
            }
        }.view
    }

    private fun extractCoor(edit: Editable): Double {
        val text = edit.toString()
        return when {
            text.contains("+") -> {
                val (d, m, s) = text.split("+")
                d.toInt() + m.toDouble() / 60 + s.toDouble() / 3600
            }
            else -> text.toDouble()
        }
    }
}