package com.idevicesinc.sweetblue;


interface P_NativeDeviceLayerFactory<T extends P_NativeDeviceLayer>
{
    T newInstance(BleDevice device);
}
