package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleManager#setListener_UhOh(UhOhListener)}
 * to receive a callback when an {@link UhOhListener.UhOh} occurs.
 *
 * @see UhOhListener.UhOh
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface UhOhListener extends GenericListener_Void<UhOhListener.UhOhEvent>
{
    /**
     * An UhOh is a warning about an exceptional (in the bad sense) and unfixable problem with the underlying stack that
     * the app can warn its user about. It's kind of like an {@link Exception} but they can be so common
     * that using {@link Exception} would render this library unusable without a rat's nest of try/catches.
     * Instead you implement {@link UhOhListener} to receive them. Each {@link UhOhListener.UhOh} has a {@link UhOhListener.UhOh#getRemedy()}
     * that suggests what might be done about it.
     *
     * @see UhOhListener
     * @see BleManager#setListener_UhOh(UhOhListener)
     */
    public static enum UhOh
    {
        /**
         * A {@link BleTask#BOND} operation timed out. This can happen a lot with the Galaxy Tab 4, and doing {@link BleManager#reset()} seems to fix it.
         * SweetBlue does as much as it can to work around the issue that causes bond timeouts, but some might still slip through.
         */
        BOND_TIMED_OUT,

        /**
         * A {@link BleDevice#read(java.util.UUID, ReadWriteListener)}
         * took longer than timeout set by {@link BleDeviceConfig#taskTimeoutRequestFilter}.
         * You will also get a {@link ReadWriteListener.ReadWriteEvent} with {@link ReadWriteListener.Status#TIMED_OUT}
         * but a timeout is a sort of fringe case that should not regularly happen.
         */
        READ_TIMED_OUT,

        /**
         * A {@link BleDevice#read(java.util.UUID, ReadWriteListener)} returned with a <code>null</code>
         * characteristic value. The <code>null</code> value will end up as an empty array in {@link ReadWriteListener.ReadWriteEvent#data}
         * so app-land doesn't have to do any special <code>null</code> handling.
         */
        READ_RETURNED_NULL,

        /**
         * Similar to {@link #READ_TIMED_OUT} but for {@link BleDevice#write(java.util.UUID, byte[])}.
         */
        WRITE_TIMED_OUT,


        /**
         * When the underlying stack meets a race condition where {@link android.bluetooth.BluetoothAdapter#getState()} does not
         * match the value provided through {@link android.bluetooth.BluetoothAdapter#ACTION_STATE_CHANGED} with {@link android.bluetooth.BluetoothAdapter#EXTRA_STATE}.
         *
         */
        INCONSISTENT_NATIVE_BLE_STATE,

        /**
         * A {@link BleDevice} went from {@link BleDeviceState#BONDING} to {@link BleDeviceState#UNBONDED}.
         * UPDATE: This can happen under normal circumstances, so not listing it as an uh oh for now.
         */
//			WENT_FROM_BONDING_TO_UNBONDED,

        /**
         * A {@link android.bluetooth.BluetoothGatt#discoverServices()} operation returned two duplicate services. Not the same instance
         * necessarily but the same UUID.
         */
        DUPLICATE_SERVICE_FOUND,

        /**
         * A {@link android.bluetooth.BluetoothGatt#discoverServices()} operation returned a service instance that we already received before
         * after disconnecting and reconnecting.
         */
        OLD_DUPLICATE_SERVICE_FOUND,

        /**
         * {@link android.bluetooth.BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} failed for an unknown reason. The library is now using
         * {@link android.bluetooth.BluetoothAdapter#startDiscovery()} instead.
         *
         * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
         */
        START_BLE_SCAN_FAILED__USING_CLASSIC,

        /**
         * {@link android.bluetooth.BluetoothGatt#getConnectionState(BluetoothDevice)} says we're connected but we never tried to connect in the first place.
         * My theory is that this can happen on some phones when you quickly restart the app and the stack doesn't have
         * a chance to disconnect from the device entirely.
         */
        CONNECTED_WITHOUT_EVER_CONNECTING,

        /**
         * Similar in concept to {@link UhOhListener.UhOh#RANDOM_EXCEPTION} but used when {@link android.os.DeadObjectException} is thrown.
         */
        DEAD_OBJECT_EXCEPTION,

        /**
         * The underlying native BLE stack enjoys surprising you with random exceptions. Every time a new one is discovered
         * it is wrapped in a try/catch and this {@link UhOhListener.UhOh} is dispatched.
         */
        RANDOM_EXCEPTION,

        /**
         * Occasionally, when trying to get the native GattService, android will throw a ConcurrentModificationException. This can happen
         * when trying to perform any read or write. Usually, you simply have to just try again.
         */
        CONCURRENT_EXCEPTION,

        /**
         * {@link android.bluetooth.BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>false</code>.
         *
         * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
         */
        START_BLE_SCAN_FAILED,

        /**
         * {@link android.bluetooth.BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>true</code>
         * so we try {@link android.bluetooth.BluetoothAdapter#startDiscovery()} but that also fails...fun!
         */
        CLASSIC_DISCOVERY_FAILED,

        /**
         * {@link android.bluetooth.BluetoothGatt#discoverServices()} failed right off the bat and returned false.
         */
        SERVICE_DISCOVERY_IMMEDIATELY_FAILED,

        /**
         * {@link android.bluetooth.BluetoothAdapter#disable()}, through {@link BleManager#turnOff()}, is failing to complete.
         * We always end up back at {@link android.bluetooth.BluetoothAdapter#STATE_ON}.
         */
        CANNOT_DISABLE_BLUETOOTH,

        /**
         * {@link android.bluetooth.BluetoothAdapter#enable()}, through {@link BleManager#turnOn()}, is failing to complete.
         * We always end up back at {@link android.bluetooth.BluetoothAdapter#STATE_OFF}. Opposite problem of {@link #CANNOT_DISABLE_BLUETOOTH}
         */
        CANNOT_ENABLE_BLUETOOTH,

        /**
         * This can be thrown when the underlying state from {@link BluetoothManager#getConnectionState(BluetoothDevice, int)} does not match
         * the apparent condition of the device (for instance, you perform a scan, then try to connect to a device, but it reports as being connected...in this case, it cannot
         * be connected, AND advertising). It seems the values from this method are cached, so sometimes this cache gets "stuck" in the connected state. In this case, it may
         * be best to clear cache of the Bluetooth app (Sometimes called Bluetooth Cache).
         */
        INCONSISTENT_NATIVE_DEVICE_STATE,

        /**
         * Just a blanket case for when the library has to completely shrug its shoulders.
         */
        UNKNOWN_BLE_ERROR;

        /**
         * Returns the {@link UhOhListener.Remedy} for this {@link UhOhListener.UhOh}.
         */
        public UhOhListener.Remedy getRemedy()
        {
            if( this.ordinal() >= CANNOT_DISABLE_BLUETOOTH.ordinal() )
            {
                return UhOhListener.Remedy.RESTART_PHONE;
            }
            else if( this.ordinal() >= START_BLE_SCAN_FAILED.ordinal() )
            {
                return UhOhListener.Remedy.RESET_BLE;
            }
            else
            {
                return UhOhListener.Remedy.WAIT_AND_SEE;
            }
        }
    }

    /**
     * The suggested remedy for each {@link UhOhListener.UhOh}. This can be used as a proxy for the severity
     * of the issue.
     */
    public static enum Remedy
    {
        /**
         * Nothing you can really do, hopefully the library can soldier on.
         */
        WAIT_AND_SEE,

        /**
         * Calling {@link BleManager#reset()} is probably in order.
         *
         * @see BleManager#reset()
         */
        RESET_BLE,

        /**
         * Might want to notify your user that a phone restart is in order.
         */
        RESTART_PHONE;
    }

    /**
     * Struct passed to {@link UhOhListener#onEvent(UhOhListener.UhOhEvent)}.
     */
    @com.idevicesinc.sweetblue.annotations.Immutable
    public static class UhOhEvent extends com.idevicesinc.sweetblue.utils.Event
    {
        /**
         * The manager associated with the {@link UhOhListener.UhOhEvent}
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        /**
         * Returns the type of {@link UhOhListener.UhOh} that occurred.
         */
        public UhOhListener.UhOh uhOh(){  return m_uhOh;  }
        private final UhOhListener.UhOh m_uhOh;

        /**
         * Forwards {@link UhOhListener.UhOh#getRemedy()}.
         */
        public UhOhListener.Remedy remedy(){  return uhOh().getRemedy();  };

        UhOhEvent(BleManager manager, UhOhListener.UhOh uhoh)
        {
            m_manager = manager;
            m_uhOh = uhoh;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "uhOh",			uhOh(),
                            "remedy",		remedy()
                    );
        }
    }

}
