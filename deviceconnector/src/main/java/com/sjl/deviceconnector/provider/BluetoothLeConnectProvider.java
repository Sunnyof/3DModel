package com.sjl.deviceconnector.provider;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import com.sjl.deviceconnector.DeviceContext;
import com.sjl.deviceconnector.ErrorCode;
import com.sjl.deviceconnector.Waiter;
import com.sjl.deviceconnector.device.bluetooth.ble.BluetoothGattWrap;
import com.sjl.deviceconnector.device.bluetooth.ble.BluetoothLeClient;
import com.sjl.deviceconnector.device.bluetooth.ble.BluetoothLeNotifyListener;
import com.sjl.deviceconnector.device.bluetooth.ble.BluetoothLeServiceListener;
import com.sjl.deviceconnector.device.bluetooth.ble.request.BluetoothLeRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.CharacteristicReadRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.CharacteristicWriteRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.DescriptorReadRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.DescriptorWriteRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.IndicateRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.MtuRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.NotifyRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.request.RemoteRssiRequest;
import com.sjl.deviceconnector.device.bluetooth.ble.response.BluetoothLeResponse;
import com.sjl.deviceconnector.exception.ProviderTimeoutException;
import com.sjl.deviceconnector.util.LogUtils;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import androidx.annotation.RequiresApi;

/**
 * 蓝牙Ble连接提供者
 *
 * <p>连接，发现服务，发送和接收数据，断开连接</p>
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BluetoothLeConnectProvider.java
 * @time 2023/3/3 9:41
 * @copyright(C) 2023 song
 */
public class BluetoothLeConnectProvider extends BaseConnectProvider {

    private final BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private MyBluetoothGattCallback mGattCallback;

    private CountDownLatch mCountDownLatch;

    private static final Waiter DEFAULT_WAITER = new Waiter();
    private final Waiter waiter;

    private BluetoothGattWrap mBluetoothGattWrap;
    private BluetoothLeClient mBluetoothLeClient;
    private Object object = new Object();
    private BluetoothLeResponse resultTempBuffer = new BluetoothLeResponse();
    private boolean resultReceived;
    private boolean resultFailed;
    private int errorCode = -1;
    private BluetoothLeNotifyListener mBluetoothLeNotifyListener;
    private BluetoothLeServiceListener mBluetoothLeServiceListener;
    private static final int DEFAULT_RECONNECT_COUNT = 3;
    /**
     * 重连次数
     */
    private int reconnectCount = 0;
    private boolean init = false;

    /**
     * 初始化一个蓝牙Ble提供者
     *
     * @param address
     */
    public BluetoothLeConnectProvider(String address) {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            throw new NullPointerException("设备不支持蓝牙");
        }
        this.mBluetoothDevice = defaultAdapter.getRemoteDevice(address);
        this.waiter = DEFAULT_WAITER;
        initParams();
    }


    /**
     * 初始化一个蓝牙Ble提供者
     *
     * @param bluetoothDevice
     */
    public BluetoothLeConnectProvider(BluetoothDevice bluetoothDevice) {
        this.mBluetoothDevice = bluetoothDevice;
        this.waiter = DEFAULT_WAITER;
        initParams();
    }

    private void initParams() {
        mBluetoothGattWrap = new BluetoothGattWrap();
        mBluetoothLeClient = new BluetoothLeClient(mBluetoothGattWrap);
    }

    @Override
    public int open() {
        int state = getState();
        if (state == ErrorCode.ERROR_OK) {
            return state;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mGattCallback = new MyBluetoothGattCallback();
            }
            if (mGattCallback == null) {
                return ErrorCode.ERROR_NOT_SUPPORTED;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCountDownLatch = new CountDownLatch(1);
                mBluetoothGatt = mBluetoothDevice.connectGatt(DeviceContext.getContext(), false,
                        mGattCallback, BluetoothDevice.TRANSPORT_LE);
                waitOpenReady();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mCountDownLatch = new CountDownLatch(1);
                mBluetoothGatt = mBluetoothDevice.connectGatt(DeviceContext.getContext(), false, mGattCallback);
                waitOpenReady();
            }else {
                return ErrorCode.ERROR_NOT_SUPPORTED;
            }
            if (mConnectState){
                mBluetoothLeClient.setBluetoothGatt(mBluetoothGatt);
                return ErrorCode.ERROR_OK;
            }else {
                return ErrorCode.ERROR_OPEN_FAIL;
            }
        } catch (Exception e) {
            LogUtils.e("蓝牙连接异常", e);
            close();
            return ErrorCode.ERROR_FAIL;
        }finally {
            init = true;
        }
    }

    /**
     * 重连, 请先执行一次open,在进行重连
     *
     * @return
     */
    public int reconnect() {
        if (!init){

            return ErrorCode.ERROR_NOT_INIT;
        }
        if (reconnectCount <= 0) {
            reconnectCount = DEFAULT_RECONNECT_COUNT;
        }
        int tempReconnectCount = 0;
        do {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCountDownLatch = new CountDownLatch(1);
                mBluetoothGatt = mBluetoothDevice.connectGatt(DeviceContext.getContext(), false,
                        mGattCallback, BluetoothDevice.TRANSPORT_LE);
                waitOpenReady();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mCountDownLatch = new CountDownLatch(1);
                mBluetoothGatt = mBluetoothDevice.connectGatt(DeviceContext.getContext(), false, mGattCallback);
                waitOpenReady();
            }
            tempReconnectCount++;
        } while (!mConnectState && tempReconnectCount <= reconnectCount);
        if (mConnectState){
            mBluetoothLeClient.setBluetoothGatt(mBluetoothGatt);
            return ErrorCode.ERROR_OK;
        }else {
            return ErrorCode.ERROR_OPEN_FAIL;
        }
    }


    @Deprecated
    @Override
    public int write(byte[] sendParams, int timeout) {
        return ErrorCode.ERROR_NOT_SUPPORTED;
    }

    @Deprecated
    @Override
    public int read(byte[] buffer, int timeout) {
        return ErrorCode.ERROR_NOT_SUPPORTED;
    }

    @Deprecated
    @Override
    public int read(byte[] sendParams, byte[] buffer, int timeout) {
        return ErrorCode.ERROR_NOT_SUPPORTED;
    }

    /**
     * 发送Ble请求
     *
     * @param request Ble请求
     * @param timeout，单位ms
     * @return
     */
    public int sendRequest(BluetoothLeRequest request, int timeout) throws Exception {
        processRequest(request, null, timeout);
        return ErrorCode.ERROR_OK;
    }

    /**
     * 发送Ble请求,带返回数据
     *
     * @param request  Ble请求
     * @param response Ble响应
     * @param timeout  请求超时时间，单位ms
     * @return
     */
    public int sendRequest(BluetoothLeRequest request, BluetoothLeResponse response, int timeout) throws Exception {
        if (request == null) {
            throw new NullPointerException("request 不能为空.");
        }
        processRequest(request, response, timeout);
        return ErrorCode.ERROR_OK;
    }

    /**
     * 处理Ble请求
     *
     * @param request  Ble请求
     * @param response Ble响应
     * @param timeout  请求超时时间
     */
    private void processRequest(BluetoothLeRequest request, BluetoothLeResponse response, int timeout) throws InterruptedException, ExecutionException {
        int state = getState();
        if (state != ErrorCode.ERROR_OK) {
            throw new RuntimeException("连接已断开");
        }
        synchronized (object) {
            resultFailed = false;
            resultReceived = false;
            resultTempBuffer.reset();
            boolean ret;
            boolean needWait = true;
            if (request instanceof CharacteristicWriteRequest) {
                CharacteristicWriteRequest characteristicWriteRequest = (CharacteristicWriteRequest) request;
                ret = mBluetoothLeClient.writeCharacteristic(characteristicWriteRequest.getService(), characteristicWriteRequest.getCharacter(), characteristicWriteRequest.getBytes());

            } else if (request instanceof CharacteristicReadRequest) {
                CharacteristicReadRequest characteristicReadRequest = (CharacteristicReadRequest) request;
                ret = mBluetoothLeClient.readCharacteristic(characteristicReadRequest.getService(), characteristicReadRequest.getCharacter());

            } else if (request instanceof DescriptorWriteRequest) {
                DescriptorWriteRequest descriptorWriteRequest = (DescriptorWriteRequest) request;
                ret = mBluetoothLeClient.writeDescriptor(descriptorWriteRequest.getService(), descriptorWriteRequest.getCharacter()
                        , descriptorWriteRequest.getDescriptor(), descriptorWriteRequest.getBytes());

            } else if (request instanceof DescriptorReadRequest) {
                DescriptorReadRequest descriptorReadRequest = (DescriptorReadRequest) request;

                ret = mBluetoothLeClient.readDescriptor(descriptorReadRequest.getService(), descriptorReadRequest.getCharacter(), descriptorReadRequest.getDescriptor());

            } else if (request instanceof NotifyRequest) {
                needWait = false;
                NotifyRequest notifyRequest = (NotifyRequest) request;
                ret = mBluetoothLeClient.setCharacteristicNotification(notifyRequest.getService(), notifyRequest.getCharacter(), notifyRequest.isEnable());

            } else if (request instanceof IndicateRequest) {
                needWait = false;
                IndicateRequest indicateRequest = (IndicateRequest) request;
                ret = mBluetoothLeClient.setCharacteristicIndication(indicateRequest.getService(), indicateRequest.getCharacter(), indicateRequest.isEnable());

            } else if (request instanceof RemoteRssiRequest) {
                ret = mBluetoothLeClient.readRemoteRssi();

            } else if (request instanceof MtuRequest) {
                MtuRequest mtuRequest = (MtuRequest) request;
                ret = mBluetoothLeClient.requestMtu(mtuRequest.getMtu());

            } else {
                throw new RuntimeException("未知请求");
            }
            if (!ret && response != null){
                //请求失败
                resultTempBuffer.setCode(ErrorCode.ERROR_SEND);
                response.copy(resultTempBuffer);
                return;
            }
            if (!needWait){
                resultTempBuffer.setCode(ErrorCode.ERROR_OK);
            }else {
                waitRequest(timeout);
                if (resultFailed) {
                    throw new ExecutionException(new Exception("错误码：" + errorCode));
                } else if (!resultReceived) {
                    throw new ProviderTimeoutException("通讯超时");
                }
            }
            if (response != null) {
                response.copy(resultTempBuffer);
            }

        }

    }

    @Override
    public void close() {
        mBluetoothLeNotifyListener = null;
        mBluetoothLeServiceListener = null;
        resultFailed = false;
        resultReceived = false;
        resultTempBuffer.reset();
        mGattCallback = null;
        mConnectState = false;
        clearConnect();
    }

    /**
     * 清除连接
     * <p>在断开连接之后再次连接经常会出现133错误</p>
     */
    public synchronized void clearConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            refreshDeviceCache();
            mBluetoothGatt.close();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public synchronized void refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null && mBluetoothGatt != null) {
                refresh.setAccessible(true);
                boolean ret = (Boolean) refresh.invoke(mBluetoothGatt);
                LogUtils.i("refreshDeviceCache, ret:  " + ret);
            }
        } catch (Exception e) {
            LogUtils.i("refreshDeviceCache exception: " + e.getMessage());
        }
    }


    private void waitOpenReady() {
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void notifyOpenReady() {
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
            mCountDownLatch = null;
        }
    }


    private void waitRequest(int timeout) throws InterruptedException {
        waiter.waitForTimeout(object, timeout);
    }

    private void notifyRequest() {
        waiter.notifyAll(object);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void discoveredServices() {
        mBluetoothGatt.discoverServices();

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public class MyBluetoothGattCallback extends BluetoothGattCallback {

        /**
         * 连接状态回调
         *
         * @param gatt     GATT client
         * @param status   用于返回操作是否成功,会返回异常码， 如BluetoothGatt#GATT_SUCCESS,下面的方法跟这里一样
         * @param newState 返回连接状态，如BluetoothProfile#STATE_DISCONNECTED、BluetoothProfile#STATE_CONNECTED
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            /**
             *	错误代码:
             *	133 ：连接超时或未找到设备。
             *	8 ： 设备超出范围
             *	22 ：表示本地设备终止了连接
             */
            LogUtils.e("连接状态改变，status:" + status + ",newState:" + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    mConnectState = true;
                    //发现服务
                    discoveredServices();
                }else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectState = false;
                    //清除连接
                    clearConnect();
                }else {
                    LogUtils.e("重新连接");
                   /* //重新连接
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.connect();
                    }*/
                }
                notifyOpenReady();
            } else {
                close();
                notifyOpenReady();
                //连接前先断开连接,开始重连，建议外部调用open之后调用
//                reconnect();
            }

        }


        /**
         * 服务发现回调
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                mBluetoothGattWrap.addServices(gatt.getServices());
            } else {
                mBluetoothGattWrap.clear();
                LogUtils.e("服务发现失败 status:" + status);
            }
            if (mBluetoothLeServiceListener != null){
                mBluetoothLeServiceListener.onServicesDiscovered(status,mBluetoothGattWrap.getServices());
            }
        }

        /**
         * 读取特征值回调
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            synchronized (object) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    resultReceived = true;
                    resultTempBuffer.setData(characteristic.getValue());
                } else {
                    resultTempBuffer.setCode(status);
                    LogUtils.e("读取特征值失败 status:" + status);
                    errorCode = status;
                    resultFailed = true;
                }
                notifyRequest();
            }

        }

        /**
         * 特征写入回调
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            synchronized (object) {
                if (BluetoothGatt.GATT_SUCCESS == status) {

                    resultReceived = true;
                    resultTempBuffer.setData(characteristic.getValue());
                } else {
                    resultTempBuffer.setCode(status);
                    LogUtils.e("特征写入失败 status:" + status);
                    errorCode = status;
                    resultFailed = true;
                }
                notifyRequest();
            }

        }

        /**
         * 监听外设特征值改变,双向通信使用，前提是该Characteristic具有NOTIFY属性，即监听服务端参数改变时回调（外设自身修改硬件参数）
         * 当写入完特征值后，外设修改自己的特征值进行回复时，手机端会触发BluetoothGattCallback#onCharacteristicChanged()方法，获取到外设回复的值，从而实现双向通信。
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (mBluetoothLeNotifyListener  != null){
                mBluetoothLeNotifyListener.onNotify(characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue());
            }
        }


        /**
         * 描述读取回调
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            synchronized (object) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    resultReceived = true;
                    resultTempBuffer.setData(descriptor.getValue());
                } else {
                    resultTempBuffer.setCode(status);
                    LogUtils.e("描述读取失败 status:" + status);
                    errorCode = status;
                    resultFailed = true;
                }
                notifyRequest();
            }

        }

        /**
         * 描述写入回调
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            synchronized (object) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    resultReceived = true;
                    resultTempBuffer.setData(descriptor.getValue());
                } else {
                    resultTempBuffer.setCode(status);
                    LogUtils.e("描述写入失败 status:" + status);
                    errorCode = status;
                    resultFailed = true;
                }
                notifyRequest();
            }

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            synchronized (object) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    resultReceived = true;
                    resultTempBuffer.setRssi(rssi);
                } else {
                    resultTempBuffer.setCode(status);
                    LogUtils.e("读取Rssi失败 status:" + status);
                    errorCode = status;
                    resultFailed = true;
                }
                notifyRequest();
            }
        }

        /**
         * MTU修改回调
         */
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            synchronized (object) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    resultReceived = true;
                    resultTempBuffer.setMtu(mtu);
                } else {
                    resultTempBuffer.setCode(status);
                    LogUtils.e("Mtu修改失败 status:" + status);
                    errorCode = status;
                    resultFailed = true;
                }
                notifyRequest();
            }

        }

    }



    /**
     * 监听服务端发来的消息
     *
     * @param bluetoothLeNotifyListener
     */
    public void setBluetoothLeNotifyListener(BluetoothLeNotifyListener bluetoothLeNotifyListener) {
        this.mBluetoothLeNotifyListener = bluetoothLeNotifyListener;
    }

    /**
     * 服务发现监听
     * @param bluetoothLeServiceListener
     */
    public void setBluetoothLeServiceListener(BluetoothLeServiceListener bluetoothLeServiceListener) {
        this.mBluetoothLeServiceListener = bluetoothLeServiceListener;
    }

    /**
     * 设置重连次数
     * @param reconnectCount
     */
    public void setReconnectCount(int reconnectCount) {
        this.reconnectCount = reconnectCount;
    }


    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }
}
