package io.typebrook.fivemoreminutes.Dialog

import android.app.*
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.util.Log
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
import io.typebrook.fmmcore.redux.CrsState
import io.typebrook.fmmcore.redux.SetCoordExpr
import io.typebrook.fmmcore.redux.SetCrsState
import org.jetbrains.anko.*
import tw.geothings.rekotlin.StoreSubscriber
import kotlin.math.pow

/**
 * Created by pham on 2017/11/24.
 */

class CoordInputDialog : DialogFragment(), StoreSubscriber<CrsState> {

    private val crs get() = mainStore.state.crsState.crs
    private val coordExpr get() = mainStore.state.crsState.coordExpr
    private val converter get() = CoordRefSys.generateConverter(crs, WGS84)
    private val reverseConverter get() = CoordRefSys.generateConverter(WGS84, crs)

    private val originalXY get() = reverseConverter(mainStore.state.currentCamera.run { lon to lat })
    private var xValues: List<EditText> = emptyList()
    private var yValues: List<EditText> = emptyList()
    private var filledX: Double = mainStore.state.currentCamera.lon
    private var filledY: Double = mainStore.state.currentCamera.lat

    private lateinit var inputLayoutContainer: FrameLayout
    private lateinit var dmsOptionLayout: LinearLayout
    private val dmsChoices
        get() = listOf(
                Triple("度", Expression.Degree, degreeInput),
                Triple("度分", Expression.DegMin, degMinInput),
                Triple("度分秒", Expression.DMS, dmsInput))

    // reaction when coordinate reference system or
    // expression[Int|Degree|DegreeMinute|DegMinSec] changes
    override fun newState(state: CrsState) {
        dmsOptionLayout.visibility = if (crs.isLonLat) VISIBLE else INVISIBLE

        val negativeButton = (this.dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEUTRAL)
        negativeButton?.visibility = if (crs.isManaged) VISIBLE else INVISIBLE

        inputLayoutContainer.apply {
            removeAllViews()
            val xyInput = if (crs.isLonLat) dmsChoices.first { it.second == coordExpr }.third
            else intInput
            addView(xyInput.layout)
            xValues = xyInput.xInputs
            yValues = xyInput.yInputs
        }
    }

    private val crsList: List<CoordRefSys>
        get() {
            val realm = Realm.getDefaultInstance()
            val crsInRealm = realm.where<CoordRefSys>().findAll().toList()
            return listOf(WGS84, TWD97, TWD67) + crsInRealm
        }

    // positive action that animate map to the coordinates which user just filled
    private val actionGoto = action@ { _: DialogInterface ->
        val xy = try {
            val rawX = xValues
                    .mapIndexed { index, value -> value.text.toString().toDouble() / 60.0.pow(index) }
                    .reduce { left, right -> left + right }
            val rawY = yValues
                    .mapIndexed { index, value -> value.text.toString().toDouble() / 60.0.pow(index) }
                    .reduce { left, right -> left + right }
            if (crs.isLonLat && !isValidInWGS84(rawX to rawY)) throw Error()
            converter(rawX to rawY)
        } catch (e: Throwable) {
            toast("Invalid Number")
            return@action
        }

        val target = CameraState(xy.second, xy.first, mainStore.state.currentCamera.zoom)
        mainStore.state.currentMap.mapControl.animateCamera(target, 600)
        return@action
    }

    // neutral action that delete selected coordinate reference system from realm,
    // and set crs as WGS84
    private val actionDelete = action@ { _: DialogInterface ->
        if (!crs.isManaged) return@action
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val throwableCrs = crs
            mainStore.dispatch(SetCrsState(WGS84))
            throwableCrs.deleteFromRealm()
        }
    }

    // Dialog skeleton
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return alert {
            title = "請輸入座標"
            customView = layout

            positiveButton("GOTO", actionGoto)
            neutralPressed("刪除", actionDelete)
            negativeButton("離開", {})

            isCancelable = false
        }.build() as Dialog
    }

    // region subscribe/unsubscribe crsState with lifecycle
    override fun onStart() {
        mainStore.subscribe(this@CoordInputDialog) { subscription ->
            subscription.select { it.crsState }.skipRepeats()
        }
        super.onStart()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        mainStore.unsubscribe(this)
        super.onDismiss(dialog)
    }
    //endregion

    // listener for crs selection, last index is for creating new crs
    private val crsSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(p0: AdapterView<*>?) {}
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
            if (pos <= crsList.lastIndex) {
                Log.d("onItemSelected", "$pos-${crsList[pos].displayName}")
                val selectedCrs = crsList[pos]
                mainStore.dispatch(SetCrsState(selectedCrs))
            } else {
                Log.d("onItemSelected", "$pos")
                CrsCreateDialog().show(fragmentManager, null)
                dismiss()
            }
        }
    }

    // listener for expression
    private val exprSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(p0: AdapterView<*>?) {}
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
            if (!crs.isLonLat) return
            mainStore dispatch SetCoordExpr(dmsChoices[pos].second)
        }
    }

    // Dialog body
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
                        adapter = ArrayAdapter(ctx,
                                android.R.layout.simple_spinner_dropdown_item,
                                crsList.map { it.displayName } + "+ Add New")

                        onItemSelectedListener = crsSelectedListener

                        val selectedPos = crsList.indexOf(crs)
                        setSelection(selectedPos)
                    }.lparams(height = 120, width = 400)
                }
                dmsOptionLayout = linearLayout {
                    leftPadding = 8
                    textView("表示方式: ")
                    spinner {
                        adapter = ArrayAdapter(ctx,
                                android.R.layout.simple_spinner_dropdown_item,
                                dmsChoices.map { it.first })

                        onItemSelectedListener = exprSelectedListener

                        val expressions = dmsChoices.map { it.second }
                        if (coordExpr in expressions) {
                            setSelection(expressions.indexOf(coordExpr), true)
                        }
                    }.lparams(height = 120, width = 400)
                }
            }
        }.view
    }

    // region coordinate-input objects, with a view for layout and EditTexts for inputs
    interface XYInput {
        var xInputs: List<EditText>
        var yInputs: List<EditText>
        val layout: View
    }

    private val intInput by lazy {
        object : XYInput {
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = UI {
                verticalLayout {
                    val xField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        hint = originalXY.first.toInt().toString()
                    }
                    val yField = editText {
                        inputType = TYPE_CLASS_NUMBER
                        hint = originalXY.second.toInt().toString()
                    }
                    xInputs = listOf(xField)
                    yInputs = listOf(yField)
                }
            }.view as LinearLayout
        }
    }

    private val degreeInput by lazy {
        object : XYInput {
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = UI {
                verticalLayout {
                    val xField = editText {
                        inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                        hint = originalXY.first.let { "%/6f".format(it) }
                    }
                    val yField = editText {
                        inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                        hint = originalXY.second.let { "%/6f".format(it) }
                    }
                    xInputs = listOf(xField)
                    yInputs = listOf(yField)
                }
            }.view as LinearLayout
        }
    }

    private val degMinInput: XYInput by lazy {
        object : XYInput {
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = UI {
                verticalLayout {
                    linearLayout {
                        val xy = originalXY
                        val xdField = editText {
                            inputType = TYPE_CLASS_NUMBER
                            filters = arrayOf(InputFilter.LengthFilter(3))
                        }.lparams { weight = 1f }
                        val xmField = editText {
                            inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                        }.lparams { weight = 1f }
                        xInputs = listOf(xdField, xmField)
                    }
                    linearLayout {
                        val ydField = editText {
                            inputType = TYPE_CLASS_NUMBER
                            filters = arrayOf(InputFilter.LengthFilter(2))
                        }.lparams { weight = 1f }
                        val ymField = editText {
                            inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                        }.lparams { weight = 1f }
                        yInputs = listOf(ydField, ymField)
                    }
                }
            }.view as LinearLayout
        }
    }

    private val dmsInput: XYInput by lazy {
        object : XYInput {
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = UI {
                verticalLayout {
                    linearLayout {
                        val xdField = editText {
                            inputType = TYPE_CLASS_NUMBER
                            filters = arrayOf(InputFilter.LengthFilter(3))
                        }.lparams { weight = 1f }
                        val xmField = editText {
                            inputType = TYPE_CLASS_NUMBER
                            filters = arrayOf(InputFilter.LengthFilter(2))
                        }.lparams { weight = 1f }
                        val xsField = editText {
                            inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                            filters = arrayOf(InputFilter.LengthFilter(4))
                        }.lparams { weight = 1f }
                        xInputs = listOf(xdField, xmField, xsField)
                    }
                    linearLayout {
                        val ydField = editText {
                            inputType = TYPE_CLASS_NUMBER
                            filters = arrayOf(InputFilter.LengthFilter(2))
                        }.lparams { weight = 1f }
                        val ymField = editText {
                            inputType = TYPE_CLASS_NUMBER
                            filters = arrayOf(InputFilter.LengthFilter(2))
                        }.lparams { weight = 1f }
                        val ysField = editText {
                            inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                            filters = arrayOf(InputFilter.LengthFilter(4))
                        }.lparams { weight = 1f }
                        yInputs = listOf(ydField, ymField, ysField)
                    }
                }
            }.view as LinearLayout
        }
    }
    // endregion
}