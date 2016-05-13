package com.idevicesinc.sweetblue.listeners;


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_String;

public interface DeviceStateListener extends P_BaseStateListener<DeviceStateListener.StateEvent>
{

    class StateEvent extends State.ChangeEvent<BleDeviceState>
    {

        private final BleDevice mDevice;
        private final int mGattStatus;


        protected StateEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
        {
            super(oldStateBits, newStateBits, intentMask);

            mDevice = device;
            mGattStatus = gattStatus;
        }

        /**
         * The device undergoing the state change.
         */
        public BleDevice device()
        {
            return mDevice;
        }

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()
        {
            return "";
        }

        /**
         * The change in gattStatus that may have precipitated the state change, or {@link com.idevicesinc.sweetblue.utils.BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
         * For example if {@link #didEnter(State)} with {@link BleDeviceState#DISCONNECTED} is <code>true</code> and
         * {@link #didExit(State)} with {@link BleDeviceState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
         * than zero and give some further hint as to why the connection failed.
         * <br><br>
         * See {@link ConnectionFailListener.ConnectionFailEvent#gattStatus()} for more information.
         */
        public int gattStatus()
        {
            return mGattStatus;
        }

        @Override public String toString()
        {
            return Utils_String.toString(newStateBits(), BleDeviceState.VALUES());
        }
    }

    @Override
    void onEvent(StateEvent event);

}
