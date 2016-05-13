package com.idevicesinc.sweetblue;


abstract class P_Task_RequiresConnection extends P_Task_RequiresBleOn
{

    public P_Task_RequiresConnection(BleDevice device, IStateListener listener)
    {
        super(device, listener);
    }

    @Override public boolean isExecutable()
    {
        boolean shouldBeExecutable = super.isExecutable() && getDevice().is(BleDeviceState.CONNECTED);

        if (shouldBeExecutable)
        {
            if (getDevice().getNativeGatt() == null)
            {
                getManager().getLogger().e("Device says we're connected, but gatt=null");
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }
}
