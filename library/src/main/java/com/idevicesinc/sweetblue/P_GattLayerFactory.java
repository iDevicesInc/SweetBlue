package com.idevicesinc.sweetblue;


interface P_GattLayerFactory<T extends P_GattLayer>
{
    T newInstance(BleDevice device);
}
