package com.idevicesinc.sweetblue;


import java.util.UUID;

/**
 * Builder-type class used when performing reads on BLE devices.
 */
public class BleRead extends BleOp<BleRead>
{

    public BleRead()
    {
    }

    public BleRead(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleRead(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    @Override
    public final boolean isValid()
    {
        return charUuid != null;
    }

    @Override
    final BleRead createDuplicate()
    {
        return getDuplicateOp();
    }

    @Override
    final BleRead createNewOp()
    {
        return new BleRead();
    }



    /**
     * Builder class to build out a list (or array) of {@link BleRead} instances.
     */
    public final static class Builder extends BleOp.Builder<Builder, BleRead>
    {

        public Builder()
        {
            this(null, null);
        }

        public Builder(UUID characteristicUuid)
        {
            this(null, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid)
        {
            currentOp = new BleRead(serviceUuid, characteristicUuid);
        }
    }
}
