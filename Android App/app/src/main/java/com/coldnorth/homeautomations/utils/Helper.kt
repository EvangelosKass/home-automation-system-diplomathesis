package com.coldnorth.homeautomations.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

object Helper {


    fun showDialogYesNo(context: Context, title:String, text:String, yesls: (dialogInterface: DialogInterface, i: Int) -> Unit, nols: (dialogInterface: DialogInterface, i: Int) -> Unit){

        val dialogbuilder = AlertDialog.Builder(context)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes,yesls)
            .setNegativeButton(android.R.string.no, nols)

        if (text!=""){
            dialogbuilder.setMessage(text)
        }
        if(title!=""){
            dialogbuilder.setTitle(title)
        }

        val dialog= dialogbuilder.create()
        dialog.show()

    }

    fun getTimePicker(title:String): MaterialTimePicker {

        val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(10)
                .setTitleText(title)
                .build()

       return picker

    }


}