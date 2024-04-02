package com.sjl.deviceconnector.test.app;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sjl.core.app.BaseApplication;
import com.sjl.core.util.ToastUtils;
import com.sjl.core.util.log.LogUtils;
import com.sjl.deviceconnector.DeviceContext;
import com.sjl.deviceconnector.entity.SerialPortConfig;
import com.sjl.deviceconnector.test.entity.SerialPortInfo;
import com.sjl.deviceconnector.test.entity.WifiInfo;
import com.sjl.deviceconnector.test.util.SpSettingUtils;
import com.tencent.smtt.sdk.QbSdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.multidex.MultiDex;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MyApplication
 * @time 2022/6/2 15:53
 * @copyright(C) 2022 song
 */
public class MyApplication extends BaseApplication {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    @Override
    public void onCreate() {
        super.onCreate();
        initLogConfig(true);
        DeviceContext.init(this,true);
        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                ToastUtils.showLong(MyApplication.this,"Init finish");
            }

            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * @param isX5 是否使用X5内核
             */
            @Override
            public void onViewInitFinished(boolean isX5) {

            }
        });
    }



    public static SerialPortInfo getSerialPortInfo() {
        return SpSettingUtils.getSerialPortInfo();
    }

    public static WifiInfo getWifiInfo() {
        return SpSettingUtils.getWifiInfo();
    }

    public static ExecutorService getExecutor() {
        return executorService;
    }

    public static Handler getMainThreadExecutor() {
        return mainThreadHandler;
    }


    public static SerialPortConfig getSerialPortConfig(SerialPortInfo serialPortInfo) {
        if (serialPortInfo == null) {
            return null;
        }
        SerialPortConfig.Builder builder;
        if (TextUtils.isEmpty(serialPortInfo.getDevicePath())){//说明是Usb Com,只需要参数即可，串口路径不需要
            builder = SerialPortConfig.newBuilder(serialPortInfo.getBaudRate());
        }else {
            builder = SerialPortConfig.newBuilder(serialPortInfo.getDevicePath(),serialPortInfo.getBaudRate());
        }

        builder.dataBits(serialPortInfo.getDataBits());
        builder.parity(serialPortInfo.getParity());
        builder.stopBits(serialPortInfo.getStopBits());
        return builder.build();
    }



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
