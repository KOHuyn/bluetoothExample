package com.kohuyn.bluetoothexamplekotlin

import android.util.Log


/**
 * Created by KO Huyn on 1/14/2021.
 */

fun logErr(tag: String, msg: String) {
    Log.e(tag, msg)
}

fun Class<*>.logErr(msg: String) {
    Log.e(this.simpleName, msg)
}