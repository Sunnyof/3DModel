package com.sun.base

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

fun Window.showWindow() {
    val decorView: View = this.decorView
    val uiOptions = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    decorView.systemUiVisibility = uiOptions
}

fun Window.immersiveMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val lp = this.attributes
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        this.attributes = lp
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        var windowInsetsController: WindowInsetsController? =
            this.decorView.windowInsetsController
        windowInsetsController?.hide(WindowInsets.Type.statusBars())
        windowInsetsController?.hide(WindowInsets.Type.navigationBars())
    } else {
        val decorView = this.decorView
        val option =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = option
    };
}