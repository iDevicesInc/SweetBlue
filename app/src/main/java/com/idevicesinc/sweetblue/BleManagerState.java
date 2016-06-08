package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;

import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;

public enum BleManagerState implements State
{

    /**
     * Analogous to {@link BluetoothAdapter#STATE_OFF}.
     */
    OFF                     (BluetoothAdapter.STATE_OFF),

    /**
     * Analogous to {@link BluetoothAdapter#STATE_TURNING_ON}.
     */
    TURNING_ON              (BluetoothAdapter.STATE_TURNING_ON),

    /**
     * Analogous to {@link BluetoothAdapter#STATE_ON}.
     */
    ON                      (BluetoothAdapter.STATE_ON),

    /**
     * Analogous to {@link BluetoothAdapter#STATE_TURNING_OFF}.
     */
    TURNING_OFF             (BluetoothAdapter.STATE_TURNING_OFF),

    /**
     * This is the state that {@link BleManager} is in after calling {@link BleManager#startScan()} or related overloads. The {@link BleManager}
     * will only be in this state for a very short period before moving to {@link #SCANNING}.
     *
     * @see BleManager#startScan()
     */
    STARTING_SCAN,

    /**
     * This is the state that {@link BleManager} is in when scanning actually starts.
     *
     */
    SCANNING,

    /**
     * This is the state that {@link BleManager} is in when running a periodic scan ({@link BleManager#startPeriodicScan(Interval, Interval)}), and
     * is currently in the pause state, but the scan task is still "executing".
     */
    SCAN_PAUSED,

    /**
     * This is the state that {@link BleManager} is in after calling {@link BleManager#reset()}.
     */
    RESETTING,

    /**
     * This is the state that {@link BleManager} is in when there has been nothing in the queue for {@link BleManagerConfig#delayBeforeIdleMs}. The
     * {@link BleManager} will run at {@link BleManagerConfig#updateThreadIdleIntervalMs} when it is idling.
     */
    IDLE,

    /**
     * This is the state that {@link BleManager} is in when all required permissions and services (if any) are enabled for scanning to work.
     */
    READY;

    private final int mNativeCode;
    private static BleManagerState[] sValues;


    BleManagerState()
    {
        mNativeCode = 0;
    }

    BleManagerState(int nativeCode)
    {
        mNativeCode = nativeCode;
    }

    public static BleManagerState[] VALUES()
    {
        sValues = sValues != null ? sValues : values();
        return sValues;
    }

    public int getNativeCode()
    {
        return mNativeCode;
    }

    static BleManagerState get(int nativeCode)
    {
        for (int i = 0; i < VALUES().length; i++)
        {
            if (sValues[i].getNativeCode() == nativeCode)
            {
                return sValues[i];
            }
        }
        return null;
    }

    @Override public boolean didEnter(int oldStateBits, int newStateBits)
    {
        return overlaps(oldStateBits) && overlaps(newStateBits);
    }

    @Override public boolean didExit(int oldStateBits, int newStateBits)
    {
        return overlaps(oldStateBits) && !overlaps(newStateBits);
    }

    @Override public boolean isNull()
    {
        return false;
    }

    @Override public int or(BitwiseEnum state)
    {
        return bit() | state.bit();
    }

    @Override public int or(int bits)
    {
        return bit() | bits;
    }

    @Override public int bit()
    {
        return 0x1 << ordinal();
    }

    @Override public boolean overlaps(int mask)
    {
        return (bit() & mask) != 0x0;
    }
}
