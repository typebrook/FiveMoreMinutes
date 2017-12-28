package io.typebrook.fivemoreminutes.Dialog

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.Editable
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.view.View
import android.view.View.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
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
    private val converter get() = Datum.generateConverter(crs, WGS84)
    private lateinit var xValue: EditText
    private lateinit var yValue: EditText
    private lateinit var dmsOption: LinearLayout

    override fun newState(state: Datum) {
        val reverseConverter = Datum.generateConverter(WGS84, crs)
        val hintXY = mainStore.state.currentCamera.run { reverseConverter(lon to lat) }
        xValue.hint = hintXY.first.let { "%.6f".format(it) }
        yValue.hint = hintXY.second.let { "%.6f".format(it) }
        dmsOption.visibility = if (state.isLonLat) VISIBLE else INVISIBLE
    }

    private val coordList: List<Datum>
        get() {
            val realm = Realm.getDefaultInstance()
            val crsInRealm = realm.where(Datum::class.java).findAll().toList()
            return listOf(WGS84, TWD97, TWD67) + crsInRealm
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return alert {
            title = "請輸入座標"
            customView = inputBox
            positiveButton("GOTO") {
                val xy = try {
                    val rawXY = xValue.text.toString().toDouble() to yValue.text.toString().toDouble()
                    if (!isValidInWGS84(rawXY)) throw Error()
                    Datum.generateConverter(crs, WGS84)(rawXY)
                } catch (e: Throwable) {
                    toast("Invalid Number")
                    return@positiveButton
                }
                val target = CameraState(xy.second, xy.first, mainStore.state.currentCamera.zoom)
                mainStore.state.currentMap.mapControl.animateCamera(target, 600)
            }

            if (crs.isManaged) {
                negativeButton("刪除") {
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        val abandonedCrs = crs
                        mainStore.dispatch(SetProjection(WGS84))
                        abandonedCrs.deleteFromRealm()
                    }
                }
            }

            mainStore.subscribe(this@CoordInputDialog) { subscription ->
                subscription.select { it.datum }.skipRepeats()
            }
        }.build() as Dialog
    }

    private val inputBox by lazy {
        UI {
            verticalLayout {
                leftPadding = 60
                rightPadding = 60
                xValue = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }
                yValue = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }

                linearLayout {
                    leftPadding = 8
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
                    }.lparams(height = 120, width = 400)
                }
                dmsOption = linearLayout {
                    leftPadding = 8
                    textView("表示方式: ")
                    spinner {
                        val choices = listOf("度", "度分", "度分秒")
                        adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, choices)
                    }.lparams(height = 120, width = 400)
                }
            }
        }.view
    }
}