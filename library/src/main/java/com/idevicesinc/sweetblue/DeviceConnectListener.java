package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;

/**
 * Convenience listener to know if a connect call was successful or not.
 *
 * This listener will be called when<br>
 * A) the {@link BleDevice} enters the {@link BleDeviceState#INITIALIZED} state -- indicating the device is ready
 * to handle any bluetooth operations.
 * <br>
 * or
 * <br>
 * B) the {@link BleDevice} failed to connect. Note that this may fire multiple times. Basically, it's called anytime
 * a connect failure happens. In this case, you should check {@link ConnectEvent#isRetrying()} to see if SweetBlue is
 * still trying to get the {@link BleDevice} connected.
 */
public interface DeviceConnectListener extends GenericListener_Void<DeviceConnectListener.ConnectEvent>
{

    class ConnectEvent extends Event
    {

        private final BleDevice m_device;
        private final DeviceReconnectFilter.ConnectFailEvent m_failEvent;


        ConnectEvent(BleDevice device, DeviceReconnectFilter.ConnectFailEvent failEvent)
        {
            m_device = device;
            m_failEvent = failEvent;
        }


        public final @Nullable(Nullable.Prevalence.NEVER) BleDevice device()
        {
            return m_device;
        }

        /**
         * Returns the {@link com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent} instance. This will be <code>null</code> if
         * {@link #wasSuccess()} returns <code>true</code>.
         */
        public final @Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter.ConnectFailEvent failEvent()
        {
            return m_failEvent;
        }

        /**
         * Returns <code>true</code> if a connection was established to the {@link #device()}. At this point, the device is connected,
         * and in the {@link BleDeviceState#INITIALIZED} state. If this returns <code>false</code>, you can retrieve failure information
         * by calling {@link #failEvent()}.
         */
        public final boolean wasSuccess()
        {
            return m_device.is(BleDeviceState.INITIALIZED) && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM);
        }

        /**
         * Convenience method which checks if the {@link #device()} is still trying to connect or not. It just checks if the {@link #device()} is
         * in any of these 3 states: {@link BleDeviceState#RECONNECTING_SHORT_TERM}, {@link BleDeviceState#RECONNECTING_LONG_TERM},
         * {@link BleDeviceState#RETRYING_BLE_CONNECTION}
         */
        public final boolean isRetrying()
        {
            return m_device.isAny(BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RETRYING_BLE_CONNECTION);
        }
    }

}
