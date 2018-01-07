package io.typebrook.fivemoreminutes.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.util.Log
import android.view.Gravity
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

    private val crs: CoordRefSys get() = mainStore.state.crsState.crs
    private var lastCrs: CoordRefSys = crs
    private val coordExpr: Expression get() = mainStore.state.crsState.coordExpr
    private val originalXY: XYPair get() = mainStore.state.currentXY.convert(WGS84, crs)
    private var typedXY: XYPair? = null

    private lateinit var inputLayoutContainer: FrameLayout
    private lateinit var xyInput: XYInput
    private lateinit var dmsOptionLayout: LinearLayout
    private val dmsChoices = listOf(
            "度" to Expression.Degree,
            "度分" to Expression.DegMin,
            "度分秒" to Expression.DMS)

    // reaction when coordinate reference system or
    // expression[Int|Degree|DegreeMinute|DegMinSec] changes
    override fun newState(state: CrsState) {
        if (::xyInput.isInitialized && xyInput.isFilled) {
            val converter = CoordRefSys.generateConverter(lastCrs, crs)
            typedXY = converter(xyInput.xyValues)
        }
        lastCrs = crs

        dmsOptionLayout.visibility = if (crs.isLonLat) VISIBLE else INVISIBLE

        val negativeButton = (this.dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEUTRAL)
        negativeButton?.visibility = if (crs.isManaged) VISIBLE else INVISIBLE

        inputLayoutContainer.apply {
            removeAllViews()
            xyInput = when (coordExpr) {
                Expression.Int -> intInput
                Expression.Degree -> degreeInput
                Expression.DegMin -> degMinInput
                Expression.DMS -> dmsInput
            }

            addView(xyInput.layout)
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
            val (rawX, rawY) = xyInput.xyValues
            if (crs.isLonLat && !isValidInWGS84(rawX to rawY)) throw Error()
            (rawX to rawY).convert(crs, WGS84)
        } catch (e: Throwable) {
            toast("Invalid Number")
            return@action
        }

        val target = CameraState(xy.second, xy.first, mainStore.state.currentCamera.zoom)
        mainStore.state.currentMap.mapControl.animateCamera(target, 600)
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
            customView = dialogLayout

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
    private val dialogLayout by lazy {
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            leftPadding = 60
            rightPadding = 60

            inputLayoutContainer = frameLayout {}

            linearLayout {
                leftPadding = 8
                textView("座標系統: ")
                spinner {
                    adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
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
    }

    // region coordinate-input objects, with a view for dialogLayout and EditTexts for inputs
    interface XYInput {
        val xyValues: XYPair
        val layout: View
        val isFilled: Boolean
    }

    interface NormalInput : XYInput {
        var xIsPositive: Boolean
        var yIsPositive: Boolean
        var xInputs: List<EditText>
        var yInputs: List<EditText>
        override val isFilled get() = (xInputs + yInputs).filter { it.text.isNotBlank() }.isNotEmpty()

        override val xyValues: XYPair
            get() {
                val x: Double = xInputs
                        .map { it.apply { if (it.text.isBlank()) it.text.append(it.hint) } }
                        .mapIndexed { index, value -> value.text.toString().toDouble() / 60.0.pow(index) }
                        .reduce { left, right -> left + right }
                        .run { if (xIsPositive) this else this.unaryMinus() }
                val y: Double = yInputs
                        .map { it.apply { if (it.text.isBlank()) it.text.append(it.hint) } }
                        .mapIndexed { index, value -> value.text.toString().toDouble() / 60.0.pow(index) }
                        .reduce { left, right -> left + right }
                        .run { if (yIsPositive) this else this.unaryMinus() }

                return x to y
            }
    }

    private val intInput: NormalInput
        get() = object : NormalInput, LinearLayout(ctx) {
            override var xIsPositive = true
            override var yIsPositive = true
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = this

        }.apply {
            orientation = LinearLayout.VERTICAL
            val hintXY = originalXY
            val xField = editText {
                inputType = TYPE_CLASS_NUMBER
                hint = hintXY.first.toInt().toString()
                typedXY?.let { text.append(it.first.toInt().toString()) }
            }
            xInputs = listOf(xField)
            val yField = editText {
                inputType = TYPE_CLASS_NUMBER
                hint = hintXY.second.toInt().toString()
                typedXY?.let { text.append(it.second.toInt().toString()) }
            }
            yInputs = listOf(yField)
        }

    private val degreeInput: XYInput
        get() = object : NormalInput, LinearLayout(ctx) {
            override var xIsPositive = true
            override var yIsPositive = true
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = this
        }.apply {
            orientation = LinearLayout.VERTICAL
            val hintXY = originalXY
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx,
                            android.R.layout.simple_spinner_dropdown_item,
                            listOf("東經(+)", "西經(-)"))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            xIsPositive = pos == 0
                        }
                    }
                }
                val xField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(10))
                    hint = hintXY.first.let { "%.6f".format(it) }
                    text.append(typedXY?.first?.let { "%.6f".format(it) } ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 5f)
                textView("度").lparams(width = 0, weight = 1f)
                xInputs = listOf(xField)
            }
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx,
                            android.R.layout.simple_spinner_dropdown_item,
                            listOf("北緯(+)", "南緯(-)"))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            xIsPositive = pos == 0
                        }
                    }
                }
                val yField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(9))
                    hint = hintXY.second.let { "%.6f".format(it) }
                    text.append(typedXY?.second?.let { "%.6f".format(it) } ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 5f)
                textView("度").lparams(width = 0, weight = 1f)
                yInputs = listOf(yField)
            }
        }

    private val degMinInput: XYInput
        get() = object : NormalInput, LinearLayout(ctx) {
            override var xIsPositive = true
            override var yIsPositive = true
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = this
        }.apply {
            orientation = LinearLayout.VERTICAL
            val hintXY = originalXY
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx,
                            android.R.layout.simple_spinner_dropdown_item,
                            listOf("東經(+)", "西經(-)"))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            xIsPositive = pos == 0
                        }
                    }
                }
                val xInDM = hintXY.first.let(Math::abs).let(degree2DM)
                val typedXInDM = typedXY?.first?.let(Math::abs)?.let(degree2DM)
                val xdField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(3))
                    hint = xInDM.first.toString()
                    text.append(typedXInDM?.first?.toString() ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 2f)
                textView("度").lparams(width = 0, weight = 1f)
                val xmField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(6))
                    hint = xInDM.second.let { "%.3f".format(it) }
                    text.append(typedXInDM?.second?.let { "%.3f".format(it) } ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 4f)
                textView("分").lparams(width = 0, weight = 1f)
                xInputs = listOf(xdField, xmField)
            }
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx,
                            android.R.layout.simple_spinner_dropdown_item,
                            listOf("北緯(+)", "南緯(-)"))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            xIsPositive = pos == 0
                        }
                    }
                }
                val yInDM = hintXY.second.let(Math::abs).let(degree2DM)
                val typedYInDM = typedXY?.second?.let(Math::abs)?.let(degree2DM)
                val ydField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = yInDM.first.toString()
                    typedXY?.let { text.append("%.6f".format(it.second)) }
                    text.append(typedYInDM?.first?.toString() ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 2f)
                textView("度").lparams(width = 0, weight = 1f)
                val ymField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(6))
                    hint = yInDM.second.let { "%.3f".format(it) }
                    text.append(typedYInDM?.second?.let { "%.3f".format(it) } ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 4f)
                textView("分").lparams(width = 0, weight = 1f)
                yInputs = listOf(ydField, ymField)
            }
        }

    private val dmsInput: XYInput
        get() = object : NormalInput, LinearLayout(ctx) {
            override var xIsPositive = true
            override var yIsPositive = true
            override lateinit var xInputs: List<EditText>
            override lateinit var yInputs: List<EditText>
            override val layout = this
        }.apply {
            orientation = LinearLayout.VERTICAL
            val hintXY = originalXY
            spinner {
                adapter = ArrayAdapter(ctx,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("東經(+)", "西經(-)"))

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                        xIsPositive = pos == 0
                    }
                }
            }
            linearLayout {
                val xInDMS = hintXY.first.let(Math::abs).let(degree2DMS)
                val typedXInDMS = typedXY?.first?.let(Math::abs)?.let(degree2DMS)
                val xdField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(3))
                    hint = xInDMS.first.toString()
                    text.append(typedXInDMS?.first?.toString() ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 3f)
                textView("度").lparams(width = 0, weight = 1f)
                val xmField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = xInDMS.second.toString()
                    text.append(typedXInDMS?.second?.toString() ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 2f)
                textView("分").lparams(width = 0, weight = 1f)
                val xsField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(4))
                    hint = xInDMS.third.let { "%.1f".format(it) }
                    text.append(typedXInDMS?.third?.let { "%.1f".format(it) } ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 4f)
                textView("秒").lparams(width = 0, weight = 1f)
                xInputs = listOf(xdField, xmField, xsField)
            }
            spinner {
                adapter = ArrayAdapter(ctx,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("北緯(+)", "南緯(-)"))

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                        xIsPositive = pos == 0
                    }
                }
            }
            linearLayout {
                val yInDMS = hintXY.second.let(Math::abs).let(degree2DMS)
                val typedYInDMS = typedXY?.second?.let(Math::abs)?.let(degree2DMS)
                val ydField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = yInDMS.first.toString()
                    text.append(typedYInDMS?.first?.toString() ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 3f)
                textView("度").lparams(width = 0, weight = 1f)
                val ymField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = yInDMS.second.toString()
                    text.append(typedYInDMS?.second?.toString() ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 2f)
                textView("分").lparams(width = 0, weight = 1f)
                val ysField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(4))
                    hint = yInDMS.third.let { "%.1f".format(it) }
                    text.append(typedYInDMS?.third?.let { "%.1f".format(it) } ?: "")
                    gravity = Gravity.END
                }.lparams(width = 0, weight = 4f)
                textView("秒").lparams(width = 0, weight = 1f)
                yInputs = listOf(ydField, ymField, ysField)
            }
        }
// endregion
}