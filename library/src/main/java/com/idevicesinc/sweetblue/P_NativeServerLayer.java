package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import java.util.List;
import java.util.UUID;


public interface P_NativeServerLayer
{

    boolean isServerNull();
    boolean addService(BluetoothGattService service);
    void cancelConnection(BluetoothDevice device);
    void clearServices();
    void close();
    boolean connect(BluetoothDevice device, boolean autoConnect);
    BluetoothGattService getService(UUID uuid);
    List<BluetoothGattService> getServices();
    boolean notifyCharacteristicChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm);
    boolean removeService(BluetoothGattService service);
    boolean sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value);
    BluetoothGattServer getNativeServer();


}
