package com.idevicesinc.sweetblue;


// Wrapper class used to hold the early out result. This class also holds the instance of BleCharacteristicWrapper and BleDescriptorWrapper, so
// that later on in read/write/etc calls, the system doesn't have to try to grab it again, as sometimes it was causing NPEs.
final class ReadWriteEarlyOutResult
{

    public BleDevice.ReadWriteListener.ReadWriteEvent m_readWriteEvent;
    public BleCharacteristicWrapper m_characteristicWrapper;
    public BleDescriptorWrapper m_descriptorWrapper;


    public final boolean earlyOut()
    {
        return m_readWriteEvent != null;
    }

    public final boolean isCharNull()
    {
        return m_characteristicWrapper == null || m_characteristicWrapper.isNull();
    }
}
