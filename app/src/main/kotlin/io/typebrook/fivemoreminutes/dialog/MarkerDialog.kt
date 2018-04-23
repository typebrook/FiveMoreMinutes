package io.typebrook.fivemoreminutes.dialog

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import io.typebrook.fivemoreminutes.utils.markerList
import io.typebrook.fmmcore.realm.geometry.rMarker
import org.jetbrains.anko.*

class MarkerDialog : DialogFragment() {

//    val adapter = object : ArrayAdapter<rMarker>(activity, android.R.layout.select_dialog_item) {
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View = with(parent!!.context) {
//            linearLayout {
//                checkBox()
//                textView(getItem(position).name)
//            }.view()
//        }
//    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
//            AlertDialog.Builder(activity)
//                    .setAdapter(MarkerAdapter()) { _, _ -> }
//                    .setTitle(markerList.size.toString())
//                    .create()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = alert {
        title = "我的航點"
        customView { listView { adapter = markerAdapter } }
    }.build() as Dialog


    private val markerAdapter = object : BaseAdapter() {
        val list = markerList

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
                LinearLayout(activity).apply {
                    textView(getItem(position).displayName)
                    checkBox()
                }

        override fun getItem(position: Int): rMarker = list[position]
        override fun getCount(): Int = list.size
        override fun getItemId(position: Int): Long = getItem(position).hashCode().toLong()
    }


}