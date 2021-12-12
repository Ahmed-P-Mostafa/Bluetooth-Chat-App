package com.polotika.bluetoothchat

import android.content.Context
import androidx.lifecycle.ViewModel

class MainActivityVIewModel() : ViewModel() {
    var context: Context? = null
    fun init(context: Context) {
        this.context = context
    }


}