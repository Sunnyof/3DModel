package com.sun.webview

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.p2p.WifiP2pManager.NetworkInfoListener
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import com.sun.base.BaseActivity
import com.sun.webview.util.FileBase64
import com.sun.webview.util.TripleDES
import java.io.InputStreamReader


open class MainActivity : BaseActivity() {

// region Value

    private lateinit var webView: WebView;
    private var H5Content: String = ""           // 提交渲染的Html内容
    private var html = StringBuilder()           // Html读缓冲

// endregion

// region Fun ???

    private fun requestPermission() {
        var permission: Array<String?> = Array(2) { Manifest.permission.READ_EXTERNAL_STORAGE;Manifest.permission.WRITE_SETTINGS }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission, 1000)
        };
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    protected fun hideBottomUI() {
        var uiFlags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        uiFlags = if (Build.VERSION.SDK_INT >= 19) {
            uiFlags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            uiFlags or View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
        window.decorView.systemUiVisibility = uiFlags
        //解决虚拟按键弹出，无法再次隐藏的问题
        window.decorView.setOnSystemUiVisibilityChangeListener { i: Int -> hideBottomUI() }
    }


// endregion

    // --- 初始化 ---
    private fun initView() {
        loadFile("page1", "")
        initWebView("")
        //loadFile( "index", "html5-webgl-galaxy/" )
        //initWebView( "html5-webgl-galaxy/" )
    }

//region Fun Webview

    // --- 读取工程文件写入变量
    private fun loadFile(FileName: String, FilePath: String) {
        var inputStream =
            InputStreamReader(resources.assets.open(FilePath + FileName + ".htm"))   // 指向要读取的文件
        var str =
            inputStream.readLines();                                                    // 读取文件内容，内容为按行划分的字符串数组

        html = StringBuilder()
        for (st in str) {
            // 字符串数组连接成单个字符串
            html.append(st)
            html.append("\r\n")
            //Log.e("TAG", html.toString());
        }

        H5Content =
            html.toString()                                                        // 原始内容直接注入
        H5Content =
            TripleDES.desDecript(html.toString())                                  // 3DS 解码后注入
        html = StringBuilder()

        //Log.e("TAG", html.toString());
    }

    // --- Web渲染 ---
    private fun initWebView(FilePath: String) {
        var settings = webView.settings;
        settings.javaScriptEnabled = true

        //webView.loadUrl("file:///android_asset/page2.htm")                               // 从工程文件加载debug
        //return

        webView.addJavascriptInterface(this, "android");
        webView.isEnabled = false
        webView.isFocusableInTouchMode = false

        // 从字符串加载
        webView.loadDataWithBaseURL(
            "file:///android_asset/" + FilePath,
            H5Content,
            "text/html",
            "UTF-8",
            ""
        )

        H5Content = ""

    }

//endregion

//region Fun JS call Android

    // javascript API 退出程序
    @JavascriptInterface
    public fun exit() {
        this.finish()
    }

    // javascript API 加载页面
    @JavascriptInterface
    public fun H5RLoad(str: String) {
        loadFile(str, "")
        runOnUiThread {
            initWebView("")
        }
    }

    // javascript API 弹出信息
    @JavascriptInterface
    public fun PopMsg(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        Log.e("TAG", event?.action.toString() +"-"+ event?.keyCode)
        when (event?.keyCode) {
            96 -> webView.evaluateJavascript("javascript:_KeyA()") {
                setScreenBrightness(20)
            }
            97 -> webView.evaluateJavascript("javascript:_KeyB()") {}
            99 -> webView.evaluateJavascript("javascript:_KeyX()") {}
            100 -> webView.evaluateJavascript("javascript:_KeyY()") {}

            19 -> webView.evaluateJavascript("javascript:_KeyUp()") {}
            20 -> webView.evaluateJavascript("javascript:_KeyDn()") {}
            21 -> webView.evaluateJavascript("javascript:_KeyLt()") {}
            22 -> webView.evaluateJavascript("javascript:_KeyRt()") {}

            103 -> webView.evaluateJavascript("javascript:_KeyR2()"){}
            105-> webView.evaluateJavascript("javascript:_KeyR1()"){}
            102 -> webView.evaluateJavascript("javascript:_KeyL1()"){}
            104-> webView.evaluateJavascript("javascript:_KeyL2()"){}

            108 ->webView.evaluateJavascript("javascript:_KeyST()"){}
            109 ->webView.evaluateJavascript("javascript:_KeySE()"){}

        }
        return super.dispatchKeyEvent(event)
    }

//endregion

//region Fun Android call JS

    // Android call JS clickJS()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideBottomUI()
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
//            initWebView()
            webView.evaluateJavascript(
                "javascript:clickJS()"
            ) {
                Toast.makeText(MainActivity@ this, it, Toast.LENGTH_LONG).show()
            };
        }
        requestPermission()

        initView()
    }

//endregion

//region Fun library

    // 获取设备唯一标识
    public fun getSN(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Build.getSerial()
        } else {
            return Build.SERIAL
        }
    }

    // 获取剪贴板内容
    public fun GetClipboard(): String {
        return ""
    }

    // 剪贴板注入内容
    public fun SetClipboard(Str: String) {

    }

//endregion

    // region Data1
    private var C = ""
// endregion

    // region Data2
    private var C3D = ""
// endregion

// region Data3

// endregion

    /**
     * 加载文件转换Base64
     */
    fun encodeBase64(path: String): String {
        return FileBase64.encodeBase64File(path);
    }

    /**
     * 解码base64存储文件
     */
    fun decodeBase64(content: String, path: String) {
        FileBase64.decoderBase64File(content, path)
    }

    /**
     * 展示数据
     */
    fun showLog(str: String) {
        Toast.makeText(this@MainActivity, str, Toast.LENGTH_LONG).show()
    }

    /**
     * 按键事件处理
     */
    @JavascriptInterface
    fun clickA() {

    }

    @JavascriptInterface
    fun axis1X(x:Int){
        if(x>65535||x<-65535){
            Toast.makeText(this@MainActivity, "数组越界了", Toast.LENGTH_LONG).show()
            return
        }
        webView.evaluateJavascript("javascript:_Axis1X($x)"){}
    }

    @JavascriptInterface
    fun axis2X(x:Int){
        if(x>65535||x<-65535){
            Toast.makeText(this@MainActivity, "数组越界了", Toast.LENGTH_LONG).show()
            return
        }
        webView.evaluateJavascript("javascript:_Axis2X($x)"){}
    }

    @JavascriptInterface
    fun axis1Y(x:Int){
        if(x>65535||x<-65535){
            Toast.makeText(this@MainActivity, "数组越界了", Toast.LENGTH_LONG).show()
            return
        }
        webView.evaluateJavascript("javascript:_Axis1Y($x)"){}
    }

    @JavascriptInterface
    fun axis2Y(x:Int){
        if(x>65535||x<-65535){
            Toast.makeText(this@MainActivity, "数组越界了", Toast.LENGTH_LONG).show()
            return
        }
        webView.evaluateJavascript("javascript:_Axis2Y($x)"){}
    }

    @JavascriptInterface
    fun _Load(FileName: String){

    }

    @JavascriptInterface
    fun _MSG(){

    }

    @JavascriptInterface
     open fun getScreenBrightness(): Int {
        val contentResolver: ContentResolver = MainActivity@this.contentResolver
        val defVal = 125
        return Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS, defVal
        )
    }
    @JavascriptInterface
    fun setScreenBrightness(light:Int){
        setScreenManualMode()
        val contentResolver: ContentResolver = MainActivity@this.contentResolver
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS, light
        )
    }

    private fun setScreenManualMode() {
        val contentResolver: ContentResolver = MainActivity@this.contentResolver
        try {
            val mode = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(
                    contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            }
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }
    }
}

