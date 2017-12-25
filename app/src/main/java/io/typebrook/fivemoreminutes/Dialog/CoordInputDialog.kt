package io.typebrook.fivemoreminutes.Dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.Editable
import android.text.InputType.*
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import io.realm.Realm
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.SetProjection
import org.jetbrains.anko.*
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/11/24.
 */
class CoordInputDialog : DialogFragment(), StoreSubscriber<Datum> {

    private val crs get() = mainStore.state.datum
    private val converter get() = Datum.generateConverter(crs, WGS84_Degree)
    private lateinit var xValue: EditText
    private lateinit var yValue: EditText

    override fun newState(state: Datum) {
        val reverseConverter = Datum.generateConverter(WGS84_Degree, crs)
        val hintXY = mainStore.state.currentCamera.run { reverseConverter(lon to lat) }
        xValue.hint = hintXY.first.let { "%.6f".format(it) }
        yValue.hint = hintXY.second.let { "%.6f".format(it) }
    }

    private val coordList: List<Datum>
        get() {
            val realm = Realm.getDefaultInstance()
            val crsInRealm = realm.where(Datum::class.java).findAll().toList()

            return listOf(WGS84_Degree, WGS84_DMS, TWD97, TWD67) + crsInRealm
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle("請輸入座標")
                .setView(inputBox)
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
                .apply {
                    if (crs.isManaged) setNegativeButton("刪除") { _, _ ->
                        val realm = Realm.getDefaultInstance()
                        realm.executeTransaction {
                            val abandonedCrs = crs
                            mainStore.dispatch(SetProjection(WGS84_Degree))
                            abandonedCrs.deleteFromRealm()
                        }
                    }

                    mainStore.subscribe(this@CoordInputDialog) { subscription ->
                        subscription.select { it.datum }.skipRepeats()
                    }
                }
                .show()
    }

    private val inputBox by lazy {
        UI {
            verticalLayout {
                padding = 25
                xValue = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }
                yValue = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }

                linearLayout {
                    padding = 25
                    textView("座標系統: ")
                    spinner {
                        val choices = coordList.map { it.displayName } + "+ Add New"
                        adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, choices)
                        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                if (pos <= coordList.lastIndex) {
                                    val selectedProj = coordList[pos]
                                    mainStore.dispatch(SetProjection(selectedProj))
                                } else {
                                    CrsCreateDialog().show(owner.fragmentManager, null)
                                    dismiss()
                                }
                            }
                        }

                        val selectedPos = coordList.indexOf(mainStore.state.datum)
                        this@spinner.setSelection(selectedPos)
                    }
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