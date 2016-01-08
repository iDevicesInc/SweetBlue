package com.idevicesinc.sweetblue.utils;


import java.util.UUID;


public class OtaWrite
{

    private UUID charUuid;
    private UUID serviceUuid;
    private byte[] data;


    public OtaWrite(UUID serviceUuid, UUID charUuid, byte[] data)
    {
        this.serviceUuid = serviceUuid;
        this.charUuid = charUuid;
        this.data = data;
    }

    public OtaWrite(UUID charUuid, byte[] data)
    {
        this.serviceUuid = null;
        this.charUuid = charUuid;
        this.data = data;
    }

    public byte[] getData()
    {
        return data;
    }

    public UUID getServiceUuid()
    {
        return serviceUuid;
    }

    public UUID getCharUuid()
    {
        return charUuid;
    }

    public boolean hasServiceUuid()
    {
        return serviceUuid != null;
    }

}
