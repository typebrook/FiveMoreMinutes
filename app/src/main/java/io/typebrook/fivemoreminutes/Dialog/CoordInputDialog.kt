package io.typebrook.fivemoreminutes.Dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import io.realm.Realm
import io.realm.kotlin.where
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.SetProjection
import org.jetbrains.anko.*
import tw.geothings.rekotlin.StoreSubscriber
import kotlin.math.pow

/**
 * Created by pham on 2017/11/24.
 */
class CoordInputDialog : DialogFragment(), StoreSubscriber<Datum> {

    private val crs get() = mainStore.state.crs
    private val converter get() = Datum.generateConverter(crs, WGS84)
    private val reverseConverter get() = Datum.generateConverter(WGS84, crs)
    private var xValues: List<EditText> = emptyList()
    private var yValues: List<EditText> = emptyList()

    private lateinit var inputLayoutContainer: FrameLayout
    private lateinit var dmsOptionLayout: LinearLayout
    private val dmsChoices
        get() = listOf(
                "度" to (Expression.Degree to degreeInput),
                "度分" to (Expression.DegMin to degMinInput),
                "度分秒" to (Expression.DMS to dmsInput))
    private val inputLayout: LinearLayout
        get() = if (crs.isLonLat) dmsChoices
                .map { it.second }
                .first { it.first == crs.expression }.second
        else meterInput

    override fun newState(state: Datum) {
        dmsOptionLayout.visibility = if (state.isLonLat) VISIBLE else INVISIBLE

        val negativeButton = (this.dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEUTRAL)
        negativeButton?.visibility = if (state.isManaged) VISIBLE else INVISIBLE

        inputLayoutContainer.apply {
            removeAllViews()
            addView(inputLayout)
        }
    }

    private val coordList: List<Datum>
        get() {
            val realm = Realm.getDefaultInstance()
            val crsInRealm = realm.where<Datum>().findAll().toList()
            return listOf(WGS84, TWD97, TWD67) + crsInRealm
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return alert {
            title = "請輸入座標"
            customView = layout

            positiveButton("GOTO") {
                val xy = try {
                    val rawX = xValues
                            .mapIndexed { index, value -> value.text.toString().toDouble() / 60.0.pow(index) }
                            .reduce { left, right -> left + right }
                    val rawY = yValues
                            .mapIndexed { index, value -> value.text.toString().toDouble() / (60.0).pow(index) }
                            .reduce { left, right -> left + right }

                    if (crs.isLonLat && !isValidInWGS84(rawX to rawY)) throw Error()
                    converter(rawX to rawY)
                } catch (e: Throwable) {
                    toast("Invalid Number")
                    return@positiveButton
                }
                val target = CameraState(xy.second, xy.first, mainStore.state.currentCamera.zoom)
                mainStore.state.currentMap.mapControl.animateCamera(target, 600)
            }

            neutralPressed("刪除") {
                if (!crs.isManaged) return@neutralPressed
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    val abandonedCrs = crs
                    mainStore.dispatch(SetProjection(WGS84))
                    abandonedCrs.deleteFromRealm()
                }
            }

            negativeButton("離開") {}

            isCancelable = false

            mainStore.subscribe(this@CoordInputDialog) { subscription ->
                subscription.select { it.crs }
            }

        }.build() as Dialog
    }

    private val layout by lazy {
        UI {
            verticalLayout {
                leftPadding = 60
                rightPadding = 60

                inputLayoutContainer = frameLayout {}

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

                        val selectedPos = coordList.indexOf(mainStore.state.crs)
                        this@spinner.setSelection(selectedPos)
                    }.lparams(height = 120, width = 400)
                }
                dmsOptionLayout = linearLayout {
                    leftPadding = 8
                    textView("表示方式: ")
                    spinner {

                        adapter = ArrayAdapter(ctx,
                                android.R.layout.simple_spinner_dropdown_item,
                                dmsChoices.map { it.first })
                        
                        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                mainStore dispatch SetProjection(this@CoordInputDialog.crs.apply {
                                    expression = dmsChoices[pos].second.first
                                })
                            }
                        }
                    }.lparams(height = 120, width = 400)
                }
            }
        }.view
    }

    private val meterInput by lazy {
        UI {
            verticalLayout {
                textView("meter")
                val xField = editText { inputType = TYPE_CLASS_NUMBER }
                val yField = editText { inputType = TYPE_CLASS_NUMBER }
                xValues = listOf(xField)
                yValues = listOf(yField)
            }
        }.view as LinearLayout
    }

    private val degreeInput by lazy {
        UI {
            verticalLayout {
                textView("degree")
                val xField = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }
                val yField = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }
                xValues = listOf(xField)
                yValues = listOf(yField)
            }
        }.view as LinearLayout
    }

    private val degMinInput by lazy {
        UI {
            verticalLayout {
                linearLayout {
                    val xdField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(3))
                    }
                    val xmField = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }
                    xValues = listOf(xdField, xmField)
                }
                linearLayout {
                    val ydField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(2))
                    }
                    val ymField = editText { inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL }
                    yValues = listOf(ydField, ymField)
                }
            }
        }.view as LinearLayout
    }


    private val dmsInput by lazy {
        UI {
            verticalLayout {
                linearLayout {
                    val xdField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(3))
                    }
                    val xmField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(2))
                    }
                    val xsField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(2))
                    }
                    xValues = listOf(xdField, xmField, xsField)
                }
                linearLayout {
                    val ydField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(2))
                    }
                    val ymField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(2))
                    }
                    val ysField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        filters = arrayOf(InputFilter.LengthFilter(2))
                    }
                    yValues = listOf(ydField, ymField, ysField)
                }
            }
        }.view as LinearLayout
    }
}