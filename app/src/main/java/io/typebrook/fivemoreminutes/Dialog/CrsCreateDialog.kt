package io.typebrook.fivemoreminutes.Dialog

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import io.realm.Realm
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.projection.Datum
import io.typebrook.fmmcore.projection.ParameterType
import io.typebrook.fmmcore.redux.SetProjection
import org.jetbrains.anko.*

/**
 * Created by pham on 2017/11/15.
 */
class CrsCreateDialog : DialogFragment() {

    lateinit var parameterType: ParameterType
    lateinit var parameterText: EditText
    lateinit var displayName: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return alert {
            title = "創建新的座標參照系統"
            customView = createBox
            positiveButton("新增") {
                val newDatum = try {
                    Datum(parameterType.ordinal, parameterText.text.toString(), displayName.text.toString())
                } catch (e: Exception) {
                    activity.toast("Invalid Parameter")
                    CrsCreateDialog().show(fragmentManager, null)
                    return@positiveButton
                }
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    realm.copyToRealm(newDatum)
                }
                toast(if (newDatum.isLonLat) "is LonLat" else "not LonLat")
                mainStore.dispatch(SetProjection(newDatum))
            }
            negativeButton("離開") {}
        }.build() as Dialog
    }

    private val createBox by lazy {
        UI {
            verticalLayout {
                padding = 25
                displayName = editText {
                    hint = "Self-Defined Name"
                }
                linearLayout {
                    textView("使用") {
                        textSize = 18f
                    }.lparams { weight = 1f }
                    spinner {
                        gravity = 2
                        adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("代碼", "7參數"))
                        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                parameterType = ParameterType.values()[pos]
                                if (parameterType == ParameterType.Code) {
                                    parameterText.text.append("EPSG:")
                                    parameterText.hint = "EPSG:3857"
                                } else {
                                    parameterText.hint = "+proj=tmerc +lat_0=0 +lon_0=..."
                                }
                            }
                        }
                    }.lparams { weight = 2f }
                }
                parameterText = editText()
            }
        }.view
    }
}