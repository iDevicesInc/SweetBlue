package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GenericListener_Void;

/**
 * Provide an implementation to {@link BleDevice#setListener_State(DeviceStateListener)} and/or
 * {@link BleManager#setListener_DeviceState(DeviceStateListener)} to receive state change events.
 *
 * @see BleDeviceState
 * @see BleDevice#setListener_State(DeviceStateListener)
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface DeviceStateListener extends GenericListener_Void<BleDevice.StateListener.StateEvent>
{

    // TODO - Uncomment the below for version 3 (or better yet, copy it from BleDevice in case of changes)

//    /**
//     * Subclass that adds the device field.
//     */
//    @Immutable
//    public static class StateEvent extends State.ChangeEvent<BleDeviceState>
//    {
//        /**
//         * The device undergoing the state change.
//         */
//        public BleDevice device() {  return m_device;  }
//        private final BleDevice m_device;
//
//        /**
//         * Convience to return the mac address of {@link #device()}.
//         */
//        public String macAddress()  {  return m_device.getMacAddress();  }
//
//        /**
//         * The change in gattStatus that may have precipitated the state change, or {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
//         * For example if {@link #didEnter(State)} with {@link BleDeviceState#DISCONNECTED} is <code>true</code> and
//         * {@link #didExit(State)} with {@link BleDeviceState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
//         * than zero and give some further hint as to why the connection failed.
//         * <br><br>
//         * See {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#gattStatus()} for more information.
//         */
//        public int gattStatus() {  return m_gattStatus;  }
//        private final int m_gattStatus;
//
//        StateEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
//        {
//            super(oldStateBits, newStateBits, intentMask);
//
//            this.m_device = device;
//            this.m_gattStatus = gattStatus;
//        }
//
//        @Override public String toString()
//        {
//            if( device().is(RECONNECTING_SHORT_TERM) )
//            {
//                return Utils_String.toString
//                        (
//                                this.getClass(),
//                                "device",			device().getName_debug(),
//                                "entered",			Utils_String.toString(enterMask(),						BleDeviceState.VALUES()),
//                                "exited", 			Utils_String.toString(exitMask(),						BleDeviceState.VALUES()),
//                                "current",			Utils_String.toString(newStateBits(),					BleDeviceState.VALUES()),
//                                "current_native",	Utils_String.toString(device().getNativeStateMask(),	BleDeviceState.VALUES()),
//                                "gattStatus",		device().logger().gattStatus(gattStatus())
//                        );
//            }
//            else
//            {
//                return Utils_String.toString
//                        (
//                                this.getClass(),
//                                "device",			device().getName_debug(),
//                                "entered",			Utils_String.toString(enterMask(),						BleDeviceState.VALUES()),
//                                "exited", 			Utils_String.toString(exitMask(),						BleDeviceState.VALUES()),
//                                "current",			Utils_String.toString(newStateBits(),					BleDeviceState.VALUES()),
//                                "gattStatus",		device().logger().gattStatus(gattStatus())
//                        );
//            }
//        }
//    }

    /**
     * Called when a device's bitwise {@link BleDeviceState} changes. As many bits as possible are flipped at the same time.
     */
    void onEvent(final BleDevice.StateListener.StateEvent e);
}