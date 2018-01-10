package io.typebrook.fivemoreminutes.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import io.realm.Realm
import io.realm.kotlin.where
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.utils.degree2DM
import io.typebrook.fivemoreminutes.utils.degree2DMS
import io.typebrook.fivemoreminutes.utils.with
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.projection.CoordRefSys.Companion.TWD67
import io.typebrook.fmmcore.projection.CoordRefSys.Companion.TWD97
import io.typebrook.fmmcore.projection.CoordRefSys.Companion.WGS84
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.CrsState
import io.typebrook.fmmcore.redux.SetCoordExpr
import io.typebrook.fmmcore.redux.SetCrsState
import org.jetbrains.anko.*
import tw.geothings.rekotlin.StoreSubscriber
import kotlin.math.abs
import kotlin.math.pow

/**
 * Created by pham on 2017/11/24.
 */

class CoordInputDialog : DialogFragment(), StoreSubscriber<CrsState> {

    private val crs: CoordRefSys get() = mainStore.state.crsState.crs
    private val isLonLat: Boolean get() = mainStore.state.crsState.isLonLat
    private var lastCrs: CoordRefSys = crs
    private val coordExpr: Expression get() = mainStore.state.crsState.coordExpr
    private val originalXY: XYPair get() = mainStore.state.currentXY.convert(WGS84, crs)
    private var typedXY: XYPair? = null

    private lateinit var inputLayoutContainer: FrameLayout
    private lateinit var xyInput: XYInput
    private lateinit var dmsOptionLayout: LinearLayout
    private lateinit var dmsOption: Spinner
    private val dmsChoices by lazy {
        listOf(
                getString(R.string.option_degree) to Expression.Degree,
                getString(R.string.option_deg_min) to Expression.DegMin,
                getString(R.string.option_dms) to Expression.DMS)
    }

    // reaction when coordinate reference system or
    // expression[Int|Degree|DegreeMinute|DegMinSec] changes
    override fun newState(state: CrsState) {
        if (dmsOptionLayout.visibility == INVISIBLE && isLonLat) {
            val lastDmsExpr = dmsChoices[dmsOption.selectedItemPosition].second
            mainStore dispatch SetCoordExpr(lastDmsExpr)
        }

        dmsOptionLayout.visibility = if (isLonLat) VISIBLE else INVISIBLE

        if (::xyInput.isInitialized && xyInput.isFilled) {
            val lastXY = xyInput.xyValues
            if (lastXY.isValid(lastCrs)) typedXY = lastXY.convert(lastCrs, crs)
        }
        lastCrs = crs

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
            val convertedXY = (rawX to rawY).convert(crs, WGS84)
            if (!isValidInWGS84(convertedXY)) throw Error()
            convertedXY
        } catch (e: Throwable) {
            toast("Invalid Number")
            return@action
        }

        val target = CameraState(xy.second, xy.first, mainStore.state.currentCamera.zoom)
        mainStore.state.currentControl.animateCamera(target, 600)
    }

    // neutral action that delete selected coordinate reference system from realm,
    // and set crs as WGS84
    private val actionDelete = action@ { _: DialogInterface ->
        if (!crs.isManaged) return@action
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val throwableCrs = crs
            mainStore dispatch SetCrsState(WGS84)
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
                val selectedCrs = crsList[pos]
                mainStore dispatch SetCrsState(selectedCrs)
            } else {
                CrsCreateDialog().show(fragmentManager, null)
                dismiss()
            }
        }
    }

    // listener for expression
    private val exprSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(p0: AdapterView<*>?) {}
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
            if (!isLonLat) return
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
                textView(getString(R.string.spinner_coordsys))
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
                textView(getString(R.string.spinner_coordinate_expression))
                dmsOption = spinner {
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

            override val xyValues: XYPair
                get() {
                    xIsPositive = xInputs[0].run { if (text.isBlank()) hint.toString().toDouble() >= 0 else text.toString().toDouble() >= 0 }
                    yIsPositive = yInputs[0].run { if (text.isBlank()) hint.toString().toDouble() >= 0 else text.toString().toDouble() >= 0 }
                    return super.xyValues
                }
        }.apply {
            orientation = LinearLayout.VERTICAL
            val hintXY = originalXY
            val xField = editText {
                inputType = TYPE_CLASS_NUMBER
                hint = hintXY.first.toInt().toString()
                text.append(typedXY?.first?.toInt()?.toString() ?: "")
                gravity = Gravity.CENTER_HORIZONTAL
            }
            xInputs = listOf(xField)
            val yField = editText {
                inputType = TYPE_CLASS_NUMBER
                hint = hintXY.second.toInt().toString()
                text.append(typedXY?.second?.toInt()?.toString() ?: "")
                gravity = Gravity.CENTER_HORIZONTAL
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
            val hintXY = originalXY.run { abs(first) to abs(second) }
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                            listOf(getString(R.string.lonEast), getString(R.string.lonWest)))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            xIsPositive = pos == 0
                        }
                    }
                    setSelection(if (originalXY.first > 0) 0 else 1)
                }
                val xField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(10))
                    hint = hintXY.first.with("%.6f")
                    text.append(typedXY?.first?.with("%.6f") ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 5f)
                textView(getString(R.string.degree)).lparams(width = 0, weight = 1f)
                xInputs = listOf(xField)
            }
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                            listOf(getString(R.string.latNorth), getString(R.string.latSouth)))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            yIsPositive = pos == 0
                        }
                    }
                    setSelection(if (originalXY.second > 0) 0 else 1)
                }
                val yField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(9))
                    hint = hintXY.second.with("%.6f")
                    text.append(typedXY?.second?.with("%.6f") ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 5f)
                textView(getString(R.string.degree)).lparams(width = 0, weight = 1f)
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
            val hintXY = originalXY.run { abs(first) to abs(second) }
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                            listOf(getString(R.string.lonEast), getString(R.string.lonWest)))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            xIsPositive = pos == 0
                        }
                    }
                    setSelection(if (originalXY.first > 0) 0 else 1)
                }
                val xInDM = hintXY.first.let(Math::abs).let(degree2DM)
                val typedXInDM = typedXY?.first?.let(Math::abs)?.let(degree2DM)
                val xdField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(3))
                    hint = xInDM.first.toString()
                    text.append(typedXInDM?.first?.toString() ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 2f)
                textView(getString(R.string.degree)).lparams(width = 0, weight = 1f)
                val xmField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(6))
                    hint = xInDM.second.with("%.3f")
                    text.append(typedXInDM?.second?.with("%.3f") ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 4f)
                textView(getString(R.string.minute)).lparams(width = 0, weight = 1f)
                xInputs = listOf(xdField, xmField)
            }
            linearLayout {
                spinner {
                    adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                            listOf(getString(R.string.latNorth), getString(R.string.latSouth)))

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            yIsPositive = pos == 0
                        }
                    }
                    setSelection(if (originalXY.second > 0) 0 else 1)
                }
                val yInDM = hintXY.second.let(Math::abs).let(degree2DM)
                val typedYInDM = typedXY?.second?.let(Math::abs)?.let(degree2DM)
                val ydField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = yInDM.first.toString()
                    text.append(typedYInDM?.first?.toString() ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 2f)
                textView(getString(R.string.degree)).lparams(width = 0, weight = 1f)
                val ymField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(6))
                    hint = yInDM.second.with("%.3f")
                    text.append(typedYInDM?.second?.with("%.3f") ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 4f)
                textView(getString(R.string.minute)).lparams(width = 0, weight = 1f)
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
            val hintXY = originalXY.run { abs(first) to abs(second) }
            spinner {
                adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                        listOf(getString(R.string.lonEast), getString(R.string.lonWest)))

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                        xIsPositive = pos == 0
                    }
                }
                setSelection(if (originalXY.first > 0) 0 else 1)
            }.layoutParams.width = 300
            linearLayout {
                val xInDMS = hintXY.first.let(Math::abs).let(degree2DMS)
                val typedXInDMS = typedXY?.first?.let(Math::abs)?.let(degree2DMS)
                val xdField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(3))
                    hint = xInDMS.first.toString()
                    text.append(typedXInDMS?.first?.toString() ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 3f)
                textView(getString(R.string.degree)).lparams(width = 0, weight = 1f)
                val xmField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = xInDMS.second.toString()
                    text.append(typedXInDMS?.second?.toString() ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 2f)
                textView(getString(R.string.minute)).lparams(width = 0, weight = 1f)
                val xsField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(4))
                    hint = xInDMS.third.with("%.1f")
                    text.append(typedXInDMS?.third?.with("%.1f") ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 4f)
                textView(getString(R.string.second)).lparams(width = 0, weight = 1f)
                xInputs = listOf(xdField, xmField, xsField)
            }
            spinner {
                adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                        listOf(getString(R.string.latNorth), getString(R.string.latSouth)))

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                        yIsPositive = pos == 0
                    }
                }
                setSelection(if (originalXY.second > 0) 0 else 1)
            }.layoutParams.width = 300
            linearLayout {
                val yInDMS = hintXY.second.let(Math::abs).let(degree2DMS)
                val typedYInDMS = typedXY?.second?.let(Math::abs)?.let(degree2DMS)
                val ydField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = yInDMS.first.toString()
                    text.append(typedYInDMS?.first?.toString() ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 3f)
                textView(getString(R.string.degree)).lparams(width = 0, weight = 1f)
                val ymField = editText {
                    inputType = TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(2))
                    hint = yInDMS.second.toString()
                    text.append(typedYInDMS?.second?.toString() ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 2f)
                textView(getString(R.string.minute)).lparams(width = 0, weight = 1f)
                val ysField = editText {
                    inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL
                    filters = arrayOf(InputFilter.LengthFilter(4))
                    hint = yInDMS.third.with("%.1f")
                    text.append(typedYInDMS?.third?.with("%.1f") ?: "")
                    gravity = Gravity.CENTER_HORIZONTAL
                }.lparams(width = 0, weight = 4f)
                textView(getString(R.string.second)).lparams(width = 0, weight = 1f)
                yInputs = listOf(ydField, ymField, ysField)
            }
        }
// endregion
}