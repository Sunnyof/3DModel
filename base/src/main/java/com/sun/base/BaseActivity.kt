package com.sun.base

import android.app.ActivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import androidx.appcompat.app.AppCompatActivity


open class BaseActivity(resourceId:Int):AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        window.immersiveMode()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_HOME){
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        (applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager).moveTaskToFront(
            taskId, 0)
    }

}